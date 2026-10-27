package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;

@WebServlet("/appointments")
public class AppointmentController extends BaseController {
  private final AppointmentService service = new AppointmentService();

  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    r.setAttribute("appointments", new AppointmentService().getAppointments(user(r).getUserId()));
    r.setAttribute("schedules", new ScheduleService().getAvailableSchedules());
    view(r, s, "appointments");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    long patient = user(r).getUserId();
    switch (p(r, "action")) {
      case "book" -> service.createAppointment(patient, id(r, "scheduleId"), p(r, "symptoms"));
      case "reschedule" ->
          service.rescheduleAppointment(patient, id(r, "appointmentId"), id(r, "scheduleId"));
      case "cancel" -> service.cancelAppointment(patient, id(r, "appointmentId"));
      default -> throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
    }
    redirect(r, s, "/appointments", "Đã cập nhật lịch khám.");
  }
}
