package vn.hospital.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.dao.QueueDAO;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class QueueService {
  /** Lễ tân xác nhận lịch hôm nay rồi cấp số chờ, chặn tiếp nhận trùng. */
  public long checkIn(AuthUser actor, long appointment, String priority) {
    Actors.require(actor, "RECEPTIONIST", "ADMIN");
    return Database.tx(connection -> issueTicket(connection, actor, appointment, priority));
  }

  public long registerWalkIn(
      AuthUser actor, long patient, long schedule, String symptoms, String priority) {
    Actors.require(actor, "RECEPTIONIST", "ADMIN");
    return Database.tx(
        connection -> {
          long appt = new AppointmentService().book(connection, patient, schedule, symptoms, false);
          return issueTicket(connection, actor, appt, priority);
        });
  }

  /** Khóa bộ đếm theo ngày/phòng để mỗi bệnh nhân nhận một số thứ tự duy nhất. */
  private long issueTicket(Connection connection, AuthUser actor, long appointment, String priority)
      throws SQLException {
    Checks.require(
        Set.of("STANDARD", "ELDERLY", "EMERGENCY").contains(priority), "Mức ưu tiên không hợp lệ.");
    var a = new AppointmentDAO().findById(connection, appointment);
    Checks.require(
        a != null && "BOOKED".equals(a.get("status")), "Lịch không tồn tại hoặc đã tiếp nhận.");
    Checks.require(
        ((java.sql.Date) a.get("appointment_date")).toLocalDate().equals(LocalDate.now()),
        "Chỉ tiếp nhận lịch khám trong hôm nay.");
    var doc = new DoctorDAO().findActiveRoomRows(connection, a.get("doctor_id"));
    Checks.require(doc != null, "Phòng khám không khả dụng.");
    String room = (String) doc.get("room_number");
    new QueueDAO().ensureCounter(connection, room);
    var counter = new QueueDAO().lockCounterRows(connection, room);
    long n = Rows.number(counter, "last_number") + 1;
    new QueueDAO().updateCounter(connection, n, room);
    long id =
        new QueueDAO()
            .insertQueueRow(
                connection,
                appointment,
                a.get("patient_id"),
                a.get("doctor_id"),
                n,
                room,
                priority);
    new AppointmentDAO().markCheckedIn(connection, appointment);
    AuditDAO.record(connection, actor.getUserId(), "CHECK_IN", "queue", id);
    return id;
  }

  /**
   * WF01/SQ15 - Bác sĩ gọi người tiếp theo. Đầu vào: AuthUser bác sĩ. Khóa bác sĩ và chọn phiếu
   * WAITING theo mức ưu tiên/giờ hẹn. Bác sĩ không được mở đồng thời hai lượt khám. Đầu ra:
   * queueId, trạng thái IN_CONSULTATION và bệnh án nháp duy nhất; null nếu hết người chờ.
   */
  public Long callNext(AuthUser doctor) {
    Actors.require(doctor, "DOCTOR");
    return Database.tx(
        connection -> {
          new DoctorDAO().lockDoctor(connection, doctor.getUserId());
          Checks.require(
              new QueueDAO().findCurrentConsultationRows(connection, doctor.getUserId()) == null,
              "Hãy hoàn tất bệnh nhân đang khám trước.");
          var q = new QueueDAO().lockNextRows(connection, doctor.getUserId());
          if (q == null) return null;
          new QueueDAO().markInConsultation(connection, q.get("queue_id"));
          new MedicalRecordDAO()
              .createDraftForQueue(
                  connection,
                  q.get("queue_id"),
                  q.get("appointment_id"),
                  q.get("patient_id"),
                  doctor.getUserId());
          AuditDAO.record(
              connection, doctor.getUserId(), "CALL_NEXT", "queue", Rows.number(q, "queue_id"));
          return Rows.number(q, "queue_id");
        });
  }

  /**
   * SQ15 - Chuyển phòng cho người đang chờ. Đầu vào: lễ tân/quản trị, queueId và mã phòng. Kiểm tra
   * phòng hoạt động, cùng chuyên khoa. Đầu ra: lưu phòng/số chờ mới theo bộ đếm của ngày/phòng đó;
   * giữ bác sĩ đã phân công.
   */
  public void routePatient(AuthUser actor, long queue, String room) {
    Actors.require(actor, "RECEPTIONIST", "ADMIN");
    Database.tx(
        connection -> {
          var q = new QueueDAO().lockRoutingRows(connection, queue);
          Checks.require(
              q != null && "WAITING".equals(q.get("status")),
              "Chỉ được chuyển bệnh nhân đang chờ.");
          Checks.require(
              new RoomDAO().findMatchingRoomRows(connection, room, q.get("department_id")) != null,
              "Phòng phải thuộc cùng chuyên khoa và đang hoạt động.");
          new QueueDAO().ensureCounter(connection, room);
          var n = new QueueDAO().lockCounterRows(connection, room);
          long number = Rows.number(n, "last_number") + 1;
          new QueueDAO().updateCounter(connection, number, room);
          new QueueDAO().routeRow(connection, room, number, queue);
          AuditDAO.record(connection, actor.getUserId(), "ROUTE", "queue", queue);
          return null;
        });
  }

  public List<Map<String, Object>> findActiveRooms(Object... values) {
    return new QueueDAO().findActiveRooms(values);
  }

  public List<Map<String, Object>> findQueueForActor(Object... values) {
    return new QueueDAO().findQueueForActor(values);
  }
}
