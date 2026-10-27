package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;

@WebServlet("/reception")
public class ReceptionController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    String q = "%" + p(r, "keyword") + "%";
    r.setAttribute("appointments", new ReceptionService().findAppointments(q, q, q));
    r.setAttribute("patients", new ReceptionService().findPatients());
    r.setAttribute("schedules", new ScheduleService().getAvailableSchedules());
    view(r, s, "reception");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    var service = new ReceptionService();
    long queue;
    if (p(r, "action").equals("walkin"))
      queue =
          service.registerWalkIn(
              user(r), id(r, "patientId"), id(r, "scheduleId"), p(r, "symptoms"), p(r, "priority"));
    else if (p(r, "action").equals("checkin"))
      queue = service.checkIn(user(r), id(r, "appointmentId"), p(r, "priority"));
    else throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
    redirect(r, s, "/reception", "Đã cấp phiếu chờ #" + queue);
  }
}
