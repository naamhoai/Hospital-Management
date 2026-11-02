package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.util.BusinessException;

@WebServlet("/appointments")
public class AppointmentController extends BaseController {
  private final AppointmentService service = new AppointmentService();

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setAttribute("appointments", service.getAppointments(user(request).getUserId()));
    request.setAttribute("schedules", new ScheduleService().getAvailableSchedules());
    view(request, response, "appointments");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    long patient = user(request).getUserId();
    switch (param(request, "action")) {
      case "book" ->
          service.createAppointment(patient, id(request, "scheduleId"), param(request, "symptoms"));
      case "reschedule" ->
          service.rescheduleAppointment(
              patient, id(request, "appointmentId"), id(request, "scheduleId"));
      case "cancel" -> service.cancelAppointment(patient, id(request, "appointmentId"));
      default -> throw new BusinessException("Thao tác không hợp lệ.");
    }
    redirect(request, response, "/appointments", "Đã cập nhật lịch khám.");
  }
}
