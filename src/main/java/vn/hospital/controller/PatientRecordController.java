package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.util.Actors;

@WebServlet("/my-records")
public class PatientRecordController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    new PatientRecordService(user(request))
        .workspace(param(request, "id").isBlank() ? null : Actors.id(id(request, "id")))
        .forEach(request::setAttribute);
    view(request, response, "my-records");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.sendRedirect(
        new PaymentService()
            .createLink(user(request), id(request, "invoiceId"), request.getRemoteAddr()));
  }
}
