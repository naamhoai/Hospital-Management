package vn.hospital.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import vn.hospital.model.AuthUser;

@WebFilter("/*")
public class SecurityFilter implements Filter {
  private static final Map<String, Set<String>> ACCESS =
      Map.ofEntries(
          Map.entry("/my-records", Set.of("PATIENT")),
          Map.entry("/appointments", Set.of("PATIENT")),
          Map.entry("/feedback", Set.of("PATIENT")),
          Map.entry("/services", Set.of("ADMIN", "MANAGER")),
          Map.entry("/billing", Set.of("CASHIER", "ADMIN")),
          Map.entry("/schedules", Set.of("DOCTOR", "ADMIN")),
          Map.entry("/reception", Set.of("RECEPTIONIST", "ADMIN")),
          Map.entry("/queue", Set.of("RECEPTIONIST", "DOCTOR", "ADMIN")),
          Map.entry("/emr", Set.of("DOCTOR")),
          Map.entry("/prescriptions", Set.of("DOCTOR", "PATIENT")),
          Map.entry("/clinical-orders", Set.of("DOCTOR")),
          Map.entry("/attachments", Set.of("DOCTOR", "LAB_TECHNICIAN", "PATIENT")),
          Map.entry("/lab", Set.of("LAB_TECHNICIAN")),
          Map.entry("/reports", Set.of("MANAGER", "ADMIN")));

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var r = (HttpServletRequest) request;
    var s = (HttpServletResponse) response;
    r.setCharacterEncoding("UTF-8");
    s.setCharacterEncoding("UTF-8");
    s.setHeader("X-Content-Type-Options", "nosniff");
    s.setHeader("X-Frame-Options", "DENY");
    s.setHeader("Referrer-Policy", "same-origin");
    s.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:; form-action"
            + " 'self'; frame-ancestors 'none'");
    String path = r.getServletPath();
    if (path.startsWith("/assets/")) {
      chain.doFilter(r, s);
      return;
    }
    s.setHeader("Cache-Control", "no-store");
    var session = r.getSession(false);
    AuthUser user = session == null ? null : (AuthUser) session.getAttribute("user");
    boolean publicPage =
        Set.of("/login", "/search", "/payment/return", "/payment/ipn").contains(path);
    if ("POST".equals(r.getMethod())) {
      Object expected = session == null ? null : session.getAttribute("csrf");
      String submitted;
      try {
        submitted = r.getParameter("csrf");
      } catch (IllegalStateException e) {
        s.setStatus(413);
        r.setAttribute("error", "Tệp đính kèm vượt giới hạn 5 MB. Vui lòng chọn tệp nhỏ hơn.");
        r.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(r, s);
        return;
      }
      boolean valid = expected instanceof String token && token.equals(submitted);
      if (!valid) {
        if (user == null) {
          // A 303 changes the stale POST into a fresh GET; never replay submitted data.
          s.setStatus(HttpServletResponse.SC_SEE_OTHER);
          s.setHeader("Location", r.getContextPath() + "/login?reason=session-expired");
        } else if (path.equals("/login") || path.equals("/logout")) {
          session.setAttribute(
              "flash",
              "Form đã cũ. Bạn vẫn đang đăng nhập; hãy thực hiện lại thao tác từ trang hiện tại.");
          s.setStatus(HttpServletResponse.SC_SEE_OTHER);
          s.setHeader("Location", r.getContextPath() + "/dashboard");
        } else {
          s.setStatus(HttpServletResponse.SC_FORBIDDEN);
          r.setAttribute(
              "error",
              "Form đã hết hạn hoặc được mở trước lần đăng nhập gần nhất. Thao tác chưa được thực"
                  + " hiện; mở lại chức năng từ thanh điều hướng và thử lại.");
          r.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(r, s);
        }
        return;
      }
    }
    if (session == null) session = r.getSession(true);
    if (session.getAttribute("csrf") == null)
      session.setAttribute("csrf", UUID.randomUUID().toString());
    if (!publicPage && user == null) {
      s.sendRedirect(r.getContextPath() + "/login");
      return;
    }
    if (ACCESS.containsKey(path) && (user == null || !ACCESS.get(path).contains(user.getRole()))) {
      s.sendError(403, "Bạn không có quyền truy cập chức năng này.");
      return;
    }
    chain.doFilter(r, s);
  }
}
