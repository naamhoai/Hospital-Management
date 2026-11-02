package vn.hospital.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class ScheduleService {
  /**
   * SQ11 - Tạo ca khám một ngày. Đầu vào: bác sĩ đăng nhập, ngày/giờ, tên ca và số chỗ. Xử lý: kiểm
   * tra quyền, ngày giờ và trùng lịch; khóa bác sĩ trước khi ghi. Đầu ra: lưu một dòng
   * work_schedule; lỗi thì không lưu ca nào.
   */
  public void createSchedule(
      AuthUser actor,
      long doctor,
      String date,
      String shift,
      String start,
      String end,
      int capacity) {
    createRecurringSchedule(actor, doctor, date, date, "ONCE", shift, start, end, capacity);
  }

  /**
   * WF02 - Lập lịch tuần/tháng từ ngày đầu đến ngày kết thúc. Mỗi lần lặp giữ ngày gốc làm mốc; ví
   * dụ ngày 31 được điều chỉnh theo số ngày của tháng. Tất cả ca được lưu trong một transaction:
   * chỉ một ca trùng cũng hoàn tác cả đợt.
   */
  public void createRecurringSchedule(
      AuthUser actor,
      long doctor,
      String firstDate,
      String untilDate,
      String recurrence,
      String shift,
      String start,
      String end,
      int capacity) {
    Actors.require(actor, "DOCTOR", "ADMIN");
    Checks.require(actor.is("ADMIN") || actor.getUserId() == doctor, "Chỉ được tạo lịch của mình.");
    LocalDate first = Checks.date(firstDate);
    LocalDate until = Checks.date(untilDate);
    Checks.require(
        !first.isBefore(LocalDate.now())
            && !until.isBefore(first)
            && !until.isAfter(first.plusYears(1)),
        "Ngày lập lịch phải từ hôm nay, trong tối đa một năm.");
    Checks.require(
        Set.of("ONCE", "WEEKLY", "MONTHLY").contains(recurrence), "Kiểu lặp không hợp lệ.");
    LocalTime from;
    LocalTime to;
    try {
      from = LocalTime.parse(start);
      to = LocalTime.parse(end);
    } catch (DateTimeException error) {
      throw new BusinessException("Giờ ca khám không hợp lệ.");
    }
    Checks.require(
        from.isBefore(to)
            && capacity > 0
            && capacity <= 240
            && capacity <= Duration.between(from, to).toMinutes(),
        "Giờ hoặc sức chứa không hợp lệ.");
    String name = Checks.text(shift, 20, "Ca khám");
    Database.tx(
        connection -> {
          Checks.require(
              new DoctorDAO().lockDoctor(connection, doctor) != null, "Không tìm thấy bác sĩ.");
          for (int index = 0; ; index++) {
            LocalDate day =
                recurrence.equals("MONTHLY") ? first.plusMonths(index) : first.plusWeeks(index);
            if (day.isAfter(until)) break;
            insertShift(connection, actor, doctor, day, name, from, to, capacity);
            if (recurrence.equals("ONCE")) break;
          }
          return null;
        });
  }

  private void insertShift(
      Connection connection,
      AuthUser actor,
      long doctor,
      LocalDate day,
      String name,
      LocalTime from,
      LocalTime to,
      int capacity)
      throws SQLException {
    var schedules = new ScheduleDAO();
    Checks.require(
        schedules.findOverlappingRows(connection, doctor, day, to, from) == null,
        "Ca khám bị trùng giờ ngày " + day + ".");
    Checks.require(
        schedules.findNamedShiftRows(connection, doctor, day, name) == null,
        "Tên ca đã tồn tại ngày " + day + ".");
    long id = schedules.insertScheduleRow(connection, doctor, day, name, from, to, capacity);
    AuditDAO.record(connection, actor.getUserId(), "CREATE_SCHEDULE", "work_schedule", id);
  }

  /** SQ13 - Nhận mã ca/sức chứa mới; chỉ đổi ca còn trống để giữ nguyên giờ hẹn đã phát hành. */
  public void adjustCapacity(AuthUser actor, long schedule, int capacity) {
    Actors.require(actor, "DOCTOR", "ADMIN");
    Database.tx(
        connection -> {
          var row = new ScheduleDAO().findById(connection, schedule);
          own(actor, row);
          Checks.require(
              Rows.number(row, "booked_count") == 0,
              "Ca đã có người đặt. Hãy tạo ca bổ sung để giữ nguyên giờ hẹn.");
          long minutes =
              Duration.between(
                      ((java.sql.Time) row.get("start_time")).toLocalTime(),
                      ((java.sql.Time) row.get("end_time")).toLocalTime())
                  .toMinutes();
          Checks.require(
              capacity > 0 && capacity <= 240 && capacity <= minutes, "Sức chứa không hợp lệ.");
          new ScheduleDAO().updateCapacity(connection, capacity, schedule);
          AuditDAO.record(
              connection, actor.getUserId(), "ADJUST_CAPACITY", "work_schedule", schedule);
          return null;
        });
  }

  /**
   * SQ12/WF02 - Gửi yêu cầu nghỉ, chưa hủy lịch hẹn. Đầu vào: mã ca của bác sĩ và lý do. Đầu ra:
   * leave_status=PENDING để quản lý xem xét. Không sửa BOOKED hay sức chứa tại đây; chỉ bước quản
   * lý duyệt mới được hủy ca.
   */
  public void requestLeave(AuthUser actor, long schedule, String reason) {
    Actors.require(actor, "DOCTOR", "ADMIN");
    String message = Checks.text(reason, 255, "Lý do nghỉ");
    Database.tx(
        connection -> {
          var row = new ScheduleDAO().findById(connection, schedule);
          own(actor, row);
          canCancel(connection, schedule, row);
          Checks.require(
              !"PENDING".equals(row.get("leave_status")), "Yêu cầu đang chờ quản lý duyệt.");
          new ScheduleDAO().requestLeave(connection, schedule, message);
          AuditDAO.record(
              connection, actor.getUserId(), "REQUEST_LEAVE", "work_schedule", schedule);
          return null;
        });
  }

  /**
   * WF02 - Quản lý duyệt/từ chối yêu cầu nghỉ. Đầu vào: mã ca, quyết định, ghi chú. Từ chối chỉ đổi
   * kết quả xét duyệt. Đồng ý: kiểm tra lại ca chưa tiếp nhận, hủy các lịch BOOKED, khóa ca và tạo
   * thông báo. Các cập nhật cùng commit để không có trường hợp hủy lịch nhưng ca vẫn nhận đặt mới.
   */
  public void reviewLeave(AuthUser actor, long schedule, boolean approved, String note) {
    Actors.require(actor, "MANAGER", "ADMIN");
    Checks.require(note != null && note.length() <= 255, "Ghi chú tối đa 255 ký tự.");
    Database.tx(
        connection -> {
          var row = new ScheduleDAO().findById(connection, schedule);
          Checks.require(
              row != null && "PENDING".equals(row.get("leave_status")),
              "Không còn yêu cầu nghỉ đang chờ duyệt.");
          if (approved) {
            canCancel(connection, schedule, row);
            for (var appointment : new AppointmentDAO().findBookedPatients(connection, schedule)) {
              AppointmentService.notify(
                  connection,
                  Rows.number(appointment, "patient_id"),
                  "Bác sĩ nghỉ ca",
                  "Lịch #"
                      + appointment.get("appointment_id")
                      + " đã hủy sau khi quản lý duyệt nghỉ. Vui lòng đặt lại.");
            }
            new AppointmentDAO().cancelScheduleAppointments(connection, schedule);
            new ScheduleDAO().cancelShift(connection, schedule);
          }
          new ScheduleDAO().reviewLeave(connection, schedule, approved, actor.getUserId(), note);
          AuditDAO.record(
              connection,
              actor.getUserId(),
              approved ? "APPROVE_LEAVE" : "REJECT_LEAVE",
              "work_schedule",
              schedule);
          return null;
        });
  }

  private void canCancel(Connection connection, long id, Map<String, Object> row)
      throws SQLException {
    Checks.require(
        "AVAILABLE".equals(row.get("status"))
            && !((java.sql.Date) row.get("work_date")).toLocalDate().isBefore(LocalDate.now()),
        "Chỉ xin nghỉ ca còn hoạt động từ hôm nay.");
    Checks.require(
        new AppointmentDAO().findReceivedPatientRows(connection, id) == null,
        "Ca đã tiếp nhận bệnh nhân, không thể hủy.");
  }

  private void own(AuthUser actor, Map<String, Object> row) {
    Checks.require(
        row != null
            && (actor.is("ADMIN") || Rows.number(row, "doctor_id") == actor.getUserId())
            && "AVAILABLE".equals(row.get("status")),
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
