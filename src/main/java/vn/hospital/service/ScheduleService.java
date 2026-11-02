package vn.hospital.service;

import java.time.*;
import java.util.List;
import java.util.Map;
import vn.hospital.dao.*;
import vn.hospital.dao.ScheduleDAO;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class ScheduleService {
  /** Kiểm tra ngày, khung giờ và lịch trùng trước khi lưu ca khám. */
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
        connection -> {
          Checks.require(
              new DoctorDAO().lockDoctor(connection, doctor) != null, "Không tìm thấy bác sĩ.");
          Checks.require(
              new ScheduleDAO().findOverlappingRows(connection, doctor, day, to, from) == null,
              "Ca khám bị trùng giờ.");
          Checks.require(
              new ScheduleDAO().findNamedShiftRows(connection, doctor, day, name) == null,
              "Tên ca đã tồn tại trong ngày.");
          long id =
              new ScheduleDAO()
                  .insertScheduleRow(connection, doctor, day, name, from, to, capacity);
          AuditDAO.record(connection, actor.getUserId(), "CREATE_SCHEDULE", "work_schedule", id);
          return null;
        });
  }

  public void adjustCapacity(AuthUser actor, long schedule, int capacity) {
    Database.tx(
        connection -> {
          var scheduleDetails = new ScheduleDAO().findById(connection, schedule);
          own(actor, scheduleDetails);
          Checks.require(
              capacity >= Rows.number(scheduleDetails, "booked_count")
                  && capacity > 0
                  && capacity <= 240,
              "Sức chứa không thể nhỏ hơn số lịch đã đặt; tối đa 240.");
          // Chỉ đổi sức chứa khi chưa có người đặt để giữ nguyên giờ của các lịch đã có.
          Checks.require(
              Rows.number(scheduleDetails, "booked_count") == 0,
              "Ca đã có lịch đặt. Hãy tạo ca bổ sung để giữ nguyên giờ hẹn.");
          Checks.require(
              capacity
                  <= Duration.between(
                          ((java.sql.Time) scheduleDetails.get("start_time")).toLocalTime(),
                          ((java.sql.Time) scheduleDetails.get("end_time")).toLocalTime())
                      .toMinutes(),
              "Sức chứa vượt thời lượng ca.");
          new ScheduleDAO().updateCapacity(connection, capacity, schedule);
          AuditDAO.record(
              connection, actor.getUserId(), "ADJUST_CAPACITY", "work_schedule", schedule);
          return null;
        });
  }

  /** Bản hiện tại hủy trực tiếp ca chưa tiếp nhận; bước quản lý duyệt nghỉ còn thiếu so với SRS. */
  public void requestLeave(AuthUser actor, long schedule) {
    Database.tx(
        connection -> {
          var scheduleDetails = new ScheduleDAO().findById(connection, schedule);
          own(actor, scheduleDetails);
          Checks.require(
              !((java.sql.Date) scheduleDetails.get("work_date"))
                  .toLocalDate()
                  .isBefore(LocalDate.now()),
              "Không hủy ca đã qua.");
          Checks.require(
              new AppointmentDAO().findReceivedPatientRows(connection, schedule) == null,
              "Ca đã tiếp nhận bệnh nhân, không thể hủy.");
          for (var a : new AppointmentDAO().findBookedPatients(connection, schedule))
            AppointmentService.notify(
                connection,
                Rows.number(a, "patient_id"),
                "Bác sĩ nghỉ ca",
                "Lịch #" + a.get("appointment_id") + " đã hủy. Vui lòng đặt lại.");
          new AppointmentDAO().cancelScheduleAppointments(connection, schedule);
          new ScheduleDAO().cancelShift(connection, schedule);
          AuditDAO.record(connection, actor.getUserId(), "LEAVE", "work_schedule", schedule);
          return null;
        });
  }

  private void own(AuthUser actor, Map<String, Object> scheduleDetails) {
    Checks.require(
        scheduleDetails != null
            && (actor.is("ADMIN") || Rows.number(scheduleDetails, "doctor_id") == actor.getUserId())
            && !"CANCELLED".equals(scheduleDetails.get("status")),
        "Không được thay đổi ca này.");
  }

  public List<Map<String, Object>> findActiveDoctors(Object... values) {
    return new ScheduleDAO().findActiveDoctors(values);
  }

  public List<Map<String, Object>> findDoctorSchedules(Object... values) {
    return new ScheduleDAO().findDoctorSchedules(values);
  }

  public List<Map<String, Object>> getAvailableSchedules() {
    return new ScheduleDAO().available();
  }
}
