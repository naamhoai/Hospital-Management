package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import vn.hospital.util.FrontendResponse;

/** Dùng lại Servlet/Service hiện có qua /api, không sao chép nghiệp vụ sang frontend. */
@WebServlet("/api/*")
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 5500000)
public class ApiController extends HttpServlet {
  private static final Set<String> ROUTES =
      Set.of(
          "/login",
          "/logout",
          "/dashboard",
          "/search",
          "/appointments",
          "/services",
          "/schedules",
          "/reception",
          "/queue",
          "/emr",
          "/prescriptions",
          "/clinical-orders",
          "/attachments",
          "/lab",
          "/result-recalls",
          "/clinical-updates",
          "/billing",
          "/reports",
          "/feedback",
          "/my-records",
          "/payment/return");

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String path = request.getPathInfo();
    if (!ROUTES.contains(path == null ? "" : path)) {
      FrontendResponse.error(request, response, 404, "Không tìm thấy chức năng.");
      return;
    }
    request.setAttribute("hospital.api", true);
    // Trả redirect thành JSON để React xử lý cả chuyển trang nội bộ và đường dẫn VNPay.
    var wrapped =
        new HttpServletResponseWrapper(response) {
          @Override
          public void sendRedirect(String location) throws IOException {
            FrontendResponse.redirect(request, this, location);
          }
        };
    request.getRequestDispatcher(path).forward(request, wrapped);
  }
}
