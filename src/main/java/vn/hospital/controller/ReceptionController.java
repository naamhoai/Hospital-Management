package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.util.BusinessException;

@WebServlet("/reception")
public class ReceptionController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String q = "%" + param(request, "keyword") + "%";
    request.setAttribute("appointments", new ReceptionService().findAppointments(q, q, q));
    request.setAttribute("patients", new ReceptionService().findPatients());
    request.setAttribute("schedules", new ScheduleService().getAvailableSchedules());
    view(request, response, "reception");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    var service = new ReceptionService();
    long queue;
    if (param(request, "action").equals("walkin"))
      queue =
          service.registerWalkIn(
              user(request),
              id(request, "patientId"),
              id(request, "scheduleId"),
              param(request, "symptoms"),
              param(request, "priority"));
    else if (param(request, "action").equals("checkin"))
      queue =
          service.checkIn(user(request), id(request, "appointmentId"), param(request, "priority"));
    else throw new BusinessException("Thao tác không hợp lệ.");
    redirect(request, response, "/reception", "Đã cấp phiếu chờ #" + queue);
  }
}
