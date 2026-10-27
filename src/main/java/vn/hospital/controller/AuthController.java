package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.AuthService;

@WebServlet(urlPatterns = {"/login", "/logout"})
public class AuthController extends BaseController {
  private final AuthService auth = new AuthService();

  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    if (user(r) != null) {
      s.sendRedirect(r.getContextPath() + "/dashboard");
      return;
    }
    if ("session-expired".equals(p(r, "reason")))
      r.setAttribute(
          "error",
          "Phiên đăng nhập hoặc form cũ đã hết hạn. Vui lòng nhập lại email và mật khẩu để đăng"
              + " nhập.");
    view(r, s, "login");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    if (r.getServletPath().equals("/logout")) {
      r.getSession().invalidate();
      s.sendRedirect(r.getContextPath() + "/login");
      return;
    }
    try {
      var u = auth.login(p(r, "email"), p(r, "password"));
      r.getSession().invalidate();
      var session = r.getSession(true);
      session.setAttribute("user", u);
      session.setAttribute("csrf", java.util.UUID.randomUUID().toString());
      s.sendRedirect(r.getContextPath() + "/dashboard");
    } catch (vn.hospital.util.BusinessException e) {
      s.setStatus(422);
      r.setAttribute("error", e.getMessage());
      view(r, s, "login");
    }
  }
}
