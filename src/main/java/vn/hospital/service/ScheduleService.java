package vn.hospital.service;

import java.time.*;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class ScheduleService {
  public void createSchedule(
      AuthUser actor,
      long doctor,
      String date,
      String shift,
      String start,
      String end,
      int capacity) {
    Checks.require(actor.is("ADMIN") || actor.getUserId() == doctor, "Chỉ được tạo lịch của mình.");
    var day = Checks.date(date);
    Checks.require(!day.isBefore(LocalDate.now()), "Không tạo ca khám trong quá khứ.");
    LocalTime from, to;
    try {
      from = LocalTime.parse(start);
      to = LocalTime.parse(end);
    } catch (Exception e) {
      throw new BusinessException("Giờ ca khám không hợp lệ.");
    }
    Checks.require(
        from.isBefore(to) && capacity > 0 && capacity <= Duration.between(from, to).toMinutes(),
        "Giờ hoặc sức chứa không hợp lệ.");
    String name = Checks.text(shift, 20, "Ca khám");
    Database.tx(
        c -> {
          Checks.require(new DoctorDAO().lockDoctor(c, doctor) != null, "Không tìm thấy bác sĩ.");
          Checks.require(
              new ScheduleDAO().findOverlappingRows(c, doctor, day, to, from) == null,
              "Ca khám bị trùng giờ.");
          Checks.require(
              new ScheduleDAO().findNamedShiftRows(c, doctor, day, name) == null,
              "Tên ca đã tồn tại trong ngày.");
          long id = new ScheduleDAO().insertScheduleRow(c, doctor, day, name, from, to, capacity);
          AuditDAO.record(c, actor.getUserId(), "CREATE_SCHEDULE", "work_schedule", id);
          return null;
        });
  }

  public void adjustCapacity(AuthUser actor, long schedule, int capacity) {
    Database.tx(
        c -> {
          var w = new ScheduleDAO().findById(c, schedule);
          own(actor, w);
          Checks.require(
              capacity >= Rows.number(w, "booked_count") && capacity > 0 && capacity <= 240,
              "Sức chứa không thể nhỏ hơn số lịch đã đặt; tối đa 240.");
          // Existing appointment times remain stable. Capacity changes are allowed before any
          // booking.
          Checks.require(
              Rows.number(w, "booked_count") == 0,
              "Ca đã có lịch đặt. Hãy tạo ca bổ sung để giữ nguyên giờ hẹn.");
          Checks.require(
              capacity
                  <= Duration.between(
                          ((java.sql.Time) w.get("start_time")).toLocalTime(),
                          ((java.sql.Time) w.get("end_time")).toLocalTime())
                      .toMinutes(),
              "Sức chứa vượt thời lượng ca.");
          new ScheduleDAO().updateCapacity(c, capacity, schedule);
          AuditDAO.record(c, actor.getUserId(), "ADJUST_CAPACITY", "work_schedule", schedule);
          return null;
        });
  }

  public void requestLeave(AuthUser actor, long schedule) {
    Database.tx(
        c -> {
          var w = new ScheduleDAO().findById(c, schedule);
          own(actor, w);
          Checks.require(
              !((java.sql.Date) w.get("work_date")).toLocalDate().isBefore(LocalDate.now()),
              "Không hủy ca đã qua.");
          Checks.require(
              new AppointmentDAO().findReceivedPatientRows(c, schedule) == null,
              "Ca đã tiếp nhận bệnh nhân, không thể hủy.");
          for (var a : new AppointmentDAO().findBookedPatients(c, schedule))
            AppointmentService.notify(
                c,
                Rows.number(a, "patient_id"),
                "Bác sĩ nghỉ ca",
                "Lịch #" + a.get("appointment_id") + " đã hủy. Vui lòng đặt lại.");
          new AppointmentDAO().cancelScheduleAppointments(c, schedule);
          new ScheduleDAO().cancelShift(c, schedule);
          AuditDAO.record(c, actor.getUserId(), "LEAVE", "work_schedule", schedule);
          return null;
        });
  }

  private void own(AuthUser actor, java.util.Map<String, Object> w) {
    Checks.require(
        w != null
            && (actor.is("ADMIN") || Rows.number(w, "doctor_id") == actor.getUserId())
            && !"CANCELLED".equals(w.get("status")),
        "Không được thay đổi ca này.");
  }

  public java.util.List<java.util.Map<String, Object>> findActiveDoctors(Object... values) {
    return new vn.hospital.dao.ScheduleDAO().findActiveDoctors(values);
  }

  public java.util.List<java.util.Map<String, Object>> findDoctorSchedules(Object... values) {
    return new vn.hospital.dao.ScheduleDAO().findDoctorSchedules(values);
  }

  public java.util.List<java.util.Map<String, Object>> getAvailableSchedules() {
    return new ScheduleDAO().available();
  }
}
