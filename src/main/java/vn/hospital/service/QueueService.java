package vn.hospital.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class QueueService {
  public long checkIn(AuthUser actor, long appointment, String priority) {
    Actors.require(actor, "RECEPTIONIST", "ADMIN");
    return Database.tx(c -> issueTicket(c, actor, appointment, priority));
  }

  public long registerWalkIn(
      AuthUser actor, long patient, long schedule, String symptoms, String priority) {
    Actors.require(actor, "RECEPTIONIST", "ADMIN");
    return Database.tx(
        c -> {
          long appt = new AppointmentService().book(c, patient, schedule, symptoms, false);
          return issueTicket(c, actor, appt, priority);
        });
  }

  private long issueTicket(Connection c, AuthUser actor, long appointment, String priority)
      throws SQLException {
    Checks.require(
        Set.of("STANDARD", "ELDERLY", "EMERGENCY").contains(priority), "Mức ưu tiên không hợp lệ.");
    var a = new AppointmentDAO().findById(c, appointment);
    Checks.require(
        a != null && "BOOKED".equals(a.get("status")), "Lịch không tồn tại hoặc đã tiếp nhận.");
    Checks.require(
        ((java.sql.Date) a.get("appointment_date")).toLocalDate().equals(LocalDate.now()),
        "Chỉ tiếp nhận lịch khám trong hôm nay.");
    var doc = new DoctorDAO().findActiveRoomRows(c, a.get("doctor_id"));
    Checks.require(doc != null, "Phòng khám không khả dụng.");
    String room = (String) doc.get("room_number");
    new QueueDAO().ensureCounter(c, room);
    var counter = new QueueDAO().lockCounterRows(c, room);
    long n = Rows.number(counter, "last_number") + 1;
    new QueueDAO().updateCounter(c, n, room);
    long id =
        new QueueDAO()
            .insertQueueRow(
                c, appointment, a.get("patient_id"), a.get("doctor_id"), n, room, priority);
    new AppointmentDAO().markCheckedIn(c, appointment);
    AuditDAO.record(c, actor.getUserId(), "CHECK_IN", "queue", id);
    return id;
  }

  public Long callNext(AuthUser doctor) {
    Actors.require(doctor, "DOCTOR");
    return Database.tx(
        c -> {
          new DoctorDAO().lockDoctor(c, doctor.getUserId());
          Checks.require(
              new QueueDAO().findCurrentConsultationRows(c, doctor.getUserId()) == null,
              "Hãy hoàn tất bệnh nhân đang khám trước.");
          var q = new QueueDAO().lockNextRows(c, doctor.getUserId());
          if (q == null) return null;
          new QueueDAO().markInConsultation(c, q.get("queue_id"));
          new MedicalRecordDAO()
              .createDraftForQueue(
                  c,
                  q.get("queue_id"),
                  q.get("appointment_id"),
                  q.get("patient_id"),
                  doctor.getUserId());
          AuditDAO.record(c, doctor.getUserId(), "CALL_NEXT", "queue", Rows.number(q, "queue_id"));
          return Rows.number(q, "queue_id");
        });
  }

  public void routePatient(AuthUser actor, long queue, String room) {
    Actors.require(actor, "RECEPTIONIST", "ADMIN");
    Database.tx(
        c -> {
          var q = new QueueDAO().lockRoutingRows(c, queue);
          Checks.require(
              q != null && "WAITING".equals(q.get("status")),
              "Chỉ được chuyển bệnh nhân đang chờ.");
          Checks.require(
              new RoomDAO().findMatchingRoomRows(c, room, q.get("department_id")) != null,
              "Phòng phải thuộc cùng chuyên khoa và đang hoạt động.");
          new QueueDAO().ensureCounter(c, room);
          var n = new QueueDAO().lockCounterRows(c, room);
          long number = Rows.number(n, "last_number") + 1;
          new QueueDAO().updateCounter(c, number, room);
          new QueueDAO().routeRow(c, room, number, queue);
          AuditDAO.record(c, actor.getUserId(), "ROUTE", "queue", queue);
          return null;
        });
  }

  public java.util.List<java.util.Map<String, Object>> findActiveRooms(Object... values) {
    return new vn.hospital.dao.QueueDAO().findActiveRooms(values);
  }

  public java.util.List<java.util.Map<String, Object>> findQueueForActor(Object... values) {
    return new vn.hospital.dao.QueueDAO().findQueueForActor(values);
  }
}
