package vn.hospital.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.util.*;

public class AppointmentService {
  private final ScheduleDAO schedules = new ScheduleDAO();
  private final AppointmentDAO appointments = new AppointmentDAO();

  /**
   * SQ06 - Bệnh nhân đặt lịch từ appointments.jsp. Đầu vào: patientId lấy từ phiên, scheduleId và
   * triệu chứng ban đầu. Khóa bệnh nhân/ca, kiểm tra giới hạn 2 lịch chưa hoàn tất và tìm giờ còn
   * trống. Đầu ra: appointmentId mới, tăng booked_count và lưu thông báo; lỗi hoàn tác tất cả.
   */
  public long createAppointment(long patient, long schedule, String symptoms) {
    return Database.tx(connection -> book(connection, patient, schedule, symptoms, true));
  }

  /**
   * Đặt lịch dùng chung cho online và khách đến trực tiếp. online=true áp dụng giới hạn đặt online;
   * lễ tân dùng false nhưng vẫn kiểm tra ca/giờ/sức chứa. Connection được truyền từ transaction bên
   * ngoài để walk-in và cấp số cùng thành công. Giá khám được chụp lúc đặt, không thay theo các lần
   * sửa danh mục sau này.
   */
  public long book(
      Connection connection, long patient, long schedule, String symptoms, boolean online)
      throws SQLException {
    var patientDetails = new PatientDAO().lockForBooking(connection, patient);
    Checks.require(patientDetails != null, "Không tìm thấy bệnh nhân.");
    if (online) {
      Checks.require(
          patientDetails.get("booking_blocked_until") == null
              || ((java.sql.Date) patientDetails.get("booking_blocked_until"))
                  .toLocalDate()
                  .isBefore(LocalDate.now()),
          "Tạm khóa đặt lịch trực tuyến 30 ngày do hủy nhiều lần.");
      long count = Rows.number(new AppointmentDAO().countIncompleteRows(connection, patient), "n");
      Checks.require(count < 2, "Bạn chỉ được có tối đa 2 lịch khám chưa hoàn tất.");
    }
    var scheduleDetails = schedules.findById(connection, schedule);
    Checks.require(
        scheduleDetails != null && "AVAILABLE".equals(scheduleDetails.get("status")),
        "Ca khám không khả dụng.");
    var slot = findFreeTime(connection, scheduleDetails, 0);
    var fee = new MedicalServiceDAO().findConsultationFeeRow(connection);
    Checks.require(fee != null, "Chưa cấu hình phí khám.");
    long id =
        new AppointmentDAO()
            .insertAppointmentRow(
                connection,
                "AP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(),
                patient,
                scheduleDetails.get("doctor_id"),
                schedule,
                scheduleDetails.get("work_date"),
                slot,
                Checks.text(symptoms, 255, "Triệu chứng"),
                fee.get("unit_price"));
    new ScheduleDAO().incrementBooked(connection, schedule);
    notify(
        connection,
        patient,
        "Xác nhận đặt lịch",
        "Mã lịch #" + id + ", ngày " + scheduleDetails.get("work_date") + " lúc " + slot);
    AuditDAO.record(connection, patient, "BOOK", "appointment", id);
    return id;
  }

  /** Chọn giờ còn trống trong ca, bỏ các giờ đã qua và các giờ đã có lịch. */
  private LocalTime findFreeTime(
      Connection connection, Map<String, Object> scheduleDetails, long exclude)
      throws SQLException {
    Checks.require(
        Rows.number(scheduleDetails, "booked_count") < Rows.number(scheduleDetails, "max_slots"),
        "Ca khám đã đầy.");
    var day = ((java.sql.Date) scheduleDetails.get("work_date")).toLocalDate();
    var start = ((Time) scheduleDetails.get("start_time")).toLocalTime();
    var end = ((Time) scheduleDetails.get("end_time")).toLocalTime();
    long minutes =
        Duration.between(start, end).toMinutes() / Rows.number(scheduleDetails, "max_slots");
    Checks.require(minutes > 0, "Sức chứa ca khám quá lớn.");
    var booked =
        new AppointmentDAO()
            .findOccupiedTimes(connection, scheduleDetails.get("schedule_id"), exclude);
    Set<LocalTime> taken = new HashSet<>();
    for (var b : booked) taken.add(((Time) b.get("appointment_time")).toLocalTime());
    for (int i = 0; i < Rows.number(scheduleDetails, "max_slots"); i++) {
      LocalTime time = start.plusMinutes(i * minutes);
      if (!taken.contains(time) && LocalDateTime.of(day, time).isAfter(LocalDateTime.now()))
        return time;
    }
    throw new BusinessException("Không còn giờ khám phù hợp trong ca này.");
  }

  /**
   * SQ07 - Đổi lịch khám. Đầu vào: bệnh nhân đang đăng nhập, mã lịch cũ và mã ca mới. Kiểm tra đúng
   * chủ lịch, trạng thái BOOKED, tối đa 2 lần và trước ca 12 giờ. Đầu ra: cập nhật ngày/giờ/bác sĩ,
   * trả chỗ ca cũ và nhận chỗ ca mới cùng transaction.
   */
  public void rescheduleAppointment(long patient, long appointment, long target) {
    Database.tx(
        connection -> {
          new PatientDAO().lockPatient(connection, patient);
          var appointmentDetails = appointments.findById(connection, appointment);
          own(appointmentDetails, patient);
          Checks.require(
              Rows.number(appointmentDetails, "schedule_id") != target, "Hãy chọn ca khám khác.");
          Checks.require(
              Rows.number(appointmentDetails, "reschedule_count") < 2,
              "Chỉ được đổi lịch tối đa 2 lần.");
          var old = schedules.findById(connection, Rows.number(appointmentDetails, "schedule_id"));
          Checks.require(
              LocalDateTime.of(
                      ((java.sql.Date) old.get("work_date")).toLocalDate(),
                      ((Time) old.get("start_time")).toLocalTime())
                  .isAfter(LocalDateTime.now().plusHours(12)),
              "Phải đổi lịch trước giờ bắt đầu ca ít nhất 12 tiếng.");
          var scheduleDetails = schedules.findById(connection, target);
          Checks.require(
              scheduleDetails != null && "AVAILABLE".equals(scheduleDetails.get("status")),
              "Ca khám không khả dụng.");
          var time = findFreeTime(connection, scheduleDetails, appointment);
          new AppointmentDAO()
              .rescheduleRow(
                  connection,
                  target,
                  scheduleDetails.get("doctor_id"),
                  scheduleDetails.get("work_date"),
                  time,
                  appointment);
          new ScheduleDAO().decrementBooked(connection, appointmentDetails.get("schedule_id"));
          new ScheduleDAO().incrementBooked(connection, target);
          notify(
              connection,
              patient,
              "Đổi lịch khám",
              "Lịch #"
                  + appointment
                  + " chuyển sang "
                  + scheduleDetails.get("work_date")
                  + " "
                  + time);
          AuditDAO.record(connection, patient, "RESCHEDULE", "appointment", appointment);
          return null;
        });
  }

  /**
   * SQ08 - Hủy lịch do bệnh nhân yêu cầu. Đầu vào: patientId từ phiên và appointmentId. Chỉ hủy
   * lịch BOOKED của chính người đó. Đầu ra: CANCELLED, trả số chỗ cho ca và tạo thông báo. Hủy
   * nhiều lần tạm khóa đặt online.
   */
  public void cancelAppointment(long patient, long appointment) {
    Database.tx(
        connection -> {
          new PatientDAO().lockPatient(connection, patient);
          var appointmentDetails = appointments.findById(connection, appointment);
          own(appointmentDetails, patient);
          new AppointmentDAO().cancelRow(connection, appointment);
          new ScheduleDAO().decrementBooked(connection, appointmentDetails.get("schedule_id"));
          long cancellations =
              Rows.number(
                  new AppointmentDAO().countMonthlyCancellationsRows(connection, patient), "n");
          if (cancellations > 3) new PatientDAO().blockBooking(connection, patient);
          notify(connection, patient, "Hủy lịch khám", "Đã hủy lịch #" + appointment);
          AuditDAO.record(connection, patient, "CANCEL", "appointment", appointment);
          return null;
        });
  }

  private void own(Map<String, Object> appointmentDetails, long patient) {
    Checks.require(
        appointmentDetails != null
            && Rows.number(appointmentDetails, "patient_id") == patient
            && "BOOKED".equals(appointmentDetails.get("status")),
        "Không thể thay đổi lịch khám này.");
  }

  public static void notify(Connection connection, long patient, String subject, String body)
      throws SQLException {
    new NotificationDAO().enqueue(connection, subject, body, patient);
  }

  public List<Map<String, Object>> getAppointments(long patient) {
    return new AppointmentDAO().findByPatient(patient);
  }
}
