package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.util.Actors;

@WebServlet("/my-records")
public class PatientRecordController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    new PatientRecordService(user(r))
        .workspace(p(r, "id").isBlank() ? null : Actors.id(id(r, "id")))
        .forEach(r::setAttribute);
    view(r, s, "my-records");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    s.sendRedirect(new PaymentService().createLink(user(r), id(r, "invoiceId"), r.getRemoteAddr()));
  }
}
