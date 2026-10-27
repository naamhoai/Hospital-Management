package vn.hospital.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.util.*;

public class AppointmentService {
  private final ScheduleDAO schedules = new ScheduleDAO();
  private final AppointmentDAO appointments = new AppointmentDAO();

  public long createAppointment(long patient, long schedule, String symptoms) {
    return Database.tx(c -> book(c, patient, schedule, symptoms, true));
  }

  public long book(Connection c, long patient, long schedule, String symptoms, boolean online)
      throws SQLException {
    var p = new PatientDAO().lockForBooking(c, patient);
    Checks.require(p != null, "Không tìm thấy bệnh nhân.");
    if (online) {
      Checks.require(
          p.get("booking_blocked_until") == null
              || ((java.sql.Date) p.get("booking_blocked_until"))
                  .toLocalDate()
                  .isBefore(LocalDate.now()),
          "Tạm khóa đặt lịch trực tuyến 30 ngày do hủy nhiều lần.");
      long count = Rows.number(new AppointmentDAO().countIncompleteRows(c, patient), "n");
      Checks.require(count < 2, "Bạn chỉ được có tối đa 2 lịch khám chưa hoàn tất.");
    }
    var w = schedules.findById(c, schedule);
    Checks.require(w != null && "AVAILABLE".equals(w.get("status")), "Ca khám không khả dụng.");
    var slot = findFreeTime(c, w, 0);
    var fee = new MedicalServiceDAO().findConsultationFeeRow(c);
    Checks.require(fee != null, "Chưa cấu hình phí khám.");
    long id =
        new AppointmentDAO()
            .insertAppointmentRow(
                c,
                "AP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(),
                patient,
                w.get("doctor_id"),
                schedule,
                w.get("work_date"),
                slot,
                Checks.text(symptoms, 255, "Triệu chứng"),
                fee.get("unit_price"));
    new ScheduleDAO().incrementBooked(c, schedule);
    notify(
        c,
        patient,
        "Xác nhận đặt lịch",
        "Mã lịch #" + id + ", ngày " + w.get("work_date") + " lúc " + slot);
    AuditDAO.record(c, patient, "BOOK", "appointment", id);
    return id;
  }

  private LocalTime findFreeTime(Connection c, Map<String, Object> w, long exclude)
      throws SQLException {
    Checks.require(Rows.number(w, "booked_count") < Rows.number(w, "max_slots"), "Ca khám đã đầy.");
    var day = ((java.sql.Date) w.get("work_date")).toLocalDate();
    var start = ((Time) w.get("start_time")).toLocalTime();
    var end = ((Time) w.get("end_time")).toLocalTime();
    long minutes = Duration.between(start, end).toMinutes() / Rows.number(w, "max_slots");
    Checks.require(minutes > 0, "Sức chứa ca khám quá lớn.");
    var booked = new AppointmentDAO().findOccupiedTimes(c, w.get("schedule_id"), exclude);
    Set<LocalTime> taken = new HashSet<>();
    for (var b : booked) taken.add(((Time) b.get("appointment_time")).toLocalTime());
    for (int i = 0; i < Rows.number(w, "max_slots"); i++) {
      LocalTime time = start.plusMinutes(i * minutes);
      if (!taken.contains(time) && LocalDateTime.of(day, time).isAfter(LocalDateTime.now()))
        return time;
    }
    throw new BusinessException("Không còn giờ khám phù hợp trong ca này.");
  }

  public void rescheduleAppointment(long patient, long appointment, long target) {
    Database.tx(
        c -> {
          new PatientDAO().lockPatient(c, patient);
          var a = appointments.findById(c, appointment);
          own(a, patient);
          Checks.require(Rows.number(a, "schedule_id") != target, "Hãy chọn ca khám khác.");
          Checks.require(Rows.number(a, "reschedule_count") < 2, "Chỉ được đổi lịch tối đa 2 lần.");
          var old = schedules.findById(c, Rows.number(a, "schedule_id"));
          Checks.require(
              LocalDateTime.of(
                      ((java.sql.Date) old.get("work_date")).toLocalDate(),
                      ((Time) old.get("start_time")).toLocalTime())
                  .isAfter(LocalDateTime.now().plusHours(12)),
              "Phải đổi lịch trước giờ bắt đầu ca ít nhất 12 tiếng.");
          var w = schedules.findById(c, target);
          Checks.require(
              w != null && "AVAILABLE".equals(w.get("status")), "Ca khám không khả dụng.");
          var time = findFreeTime(c, w, appointment);
          new AppointmentDAO()
              .rescheduleRow(c, target, w.get("doctor_id"), w.get("work_date"), time, appointment);
          new ScheduleDAO().decrementBooked(c, a.get("schedule_id"));
          new ScheduleDAO().incrementBooked(c, target);
          notify(
              c,
              patient,
              "Đổi lịch khám",
              "Lịch #" + appointment + " chuyển sang " + w.get("work_date") + " " + time);
          AuditDAO.record(c, patient, "RESCHEDULE", "appointment", appointment);
          return null;
        });
  }

  public void cancelAppointment(long patient, long appointment) {
    Database.tx(
        c -> {
          new PatientDAO().lockPatient(c, patient);
          var a = appointments.findById(c, appointment);
          own(a, patient);
          new AppointmentDAO().cancelRow(c, appointment);
          new ScheduleDAO().decrementBooked(c, a.get("schedule_id"));
          long cancellations =
              Rows.number(new AppointmentDAO().countMonthlyCancellationsRows(c, patient), "n");
          if (cancellations > 3) new PatientDAO().blockBooking(c, patient);
          notify(c, patient, "Hủy lịch khám", "Đã hủy lịch #" + appointment);
          AuditDAO.record(c, patient, "CANCEL", "appointment", appointment);
          return null;
        });
  }

  private void own(Map<String, Object> a, long patient) {
    Checks.require(
        a != null && Rows.number(a, "patient_id") == patient && "BOOKED".equals(a.get("status")),
        "Không thể thay đổi lịch khám này.");
  }

  public static void notify(Connection c, long patient, String subject, String body)
      throws SQLException {
    new NotificationDAO().enqueue(c, subject, body, patient);
  }

  public java.util.List<java.util.Map<String, Object>> getAppointments(long patient) {
    return new AppointmentDAO().findByPatient(patient);
  }
}
