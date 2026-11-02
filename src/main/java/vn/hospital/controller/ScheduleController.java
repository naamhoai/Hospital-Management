package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.service.ScheduleService;
import vn.hospital.util.BusinessException;

@WebServlet("/schedules")
public class ScheduleController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setAttribute(
        "schedules",
        new ScheduleService()
            .findDoctorSchedules(user(request).getRole(), user(request).getUserId()));
    request.setAttribute(
        "doctors",
        new ScheduleService()
            .findActiveDoctors(user(request).getRole(), user(request).getUserId()));
    view(request, response, "schedules");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    var service = new ScheduleService();
    switch (param(request, "action")) {
      case "create" ->
          service.createSchedule(
              user(request),
              id(request, "doctorId"),
              param(request, "date"),
              param(request, "shift"),
              param(request, "start"),
              param(request, "end"),
              (int) id(request, "capacity"));
      case "capacity" ->
          service.adjustCapacity(
              user(request), id(request, "scheduleId"), (int) id(request, "capacity"));
      case "leave" -> service.requestLeave(user(request), id(request, "scheduleId"));
      default -> throw new BusinessException("Thao tác không hợp lệ.");
    }
    redirect(request, response, "/schedules", "Đã cập nhật ca khám.");
  }
}
