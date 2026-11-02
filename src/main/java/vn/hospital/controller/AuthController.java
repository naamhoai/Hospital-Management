package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.AuthService;
import vn.hospital.util.BusinessException;

@WebServlet(urlPatterns = {"/login", "/logout"})
public class AuthController extends BaseController {
  private final AuthService auth = new AuthService();

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    if (user(request) != null) {
      response.sendRedirect(request.getContextPath() + "/dashboard");
      return;
    }
    if ("session-expired".equals(param(request, "reason")))
      request.setAttribute(
          "error",
          "Phiên đăng nhập hoặc form cũ đã hết hạn. Vui lòng nhập lại email và mật khẩu để đăng"
              + " nhập.");
    view(request, response, "login");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    if (request.getServletPath().equals("/logout")) {
      request.getSession().invalidate();
      response.sendRedirect(request.getContextPath() + "/login");
      return;
    }
    try {
      var u = auth.login(param(request, "email"), param(request, "password"));
      request.getSession().invalidate();
      var session = request.getSession(true);
      session.setAttribute("user", u);
      session.setAttribute("csrf", java.util.UUID.randomUUID().toString());
      response.sendRedirect(request.getContextPath() + "/dashboard");
    } catch (BusinessException e) {
      response.setStatus(422);
      request.setAttribute("error", e.getMessage());
      view(request, response, "login");
    }
  }
}
