package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.service.ScheduleService;

@WebServlet("/schedules")
public class ScheduleController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    r.setAttribute(
        "schedules",
        new ScheduleService().findDoctorSchedules(user(r).getRole(), user(r).getUserId()));
    r.setAttribute(
        "doctors", new ScheduleService().findActiveDoctors(user(r).getRole(), user(r).getUserId()));
    view(r, s, "schedules");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    var service = new ScheduleService();
    switch (p(r, "action")) {
      case "create" ->
          service.createSchedule(
              user(r),
              id(r, "doctorId"),
              p(r, "date"),
              p(r, "shift"),
              p(r, "start"),
              p(r, "end"),
              (int) id(r, "capacity"));
      case "capacity" ->
          service.adjustCapacity(user(r), id(r, "scheduleId"), (int) id(r, "capacity"));
      case "leave" -> service.requestLeave(user(r), id(r, "scheduleId"));
      default -> throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
    }
    redirect(r, s, "/schedules", "Đã cập nhật ca khám.");
  }
}
