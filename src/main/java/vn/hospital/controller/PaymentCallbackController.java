package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import vn.hospital.service.PaymentService;
import vn.hospital.util.BusinessException;
import vn.hospital.util.FrontendResponse;

@WebServlet(urlPatterns = {"/payment/return", "/payment/ipn"})
public class PaymentCallbackController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    Map<String, String> params = new HashMap<>();
    r.getParameterMap()
        .forEach(
            (k, v) -> {
              if (v.length == 1) params.put(k, v[0]);
            });
    if (r.getServletPath().endsWith("/ipn")) {
      s.setContentType("application/json");
      try {
        new PaymentService().handleCallback(params);
        s.getWriter().write("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
      } catch (BusinessException e) {
        s.getWriter().write("{\"RspCode\":\"99\",\"Message\":\"Validation failed\"}");
      }
      return;
    }
    // Trang trình duyệt chỉ tải React. React gọi API để xác nhận callback đúng một lần.
    // Nếu xác nhận ngay ở đây, callback thất bại sẽ bị gọi lại khi React tải dữ liệu.
    if (!FrontendResponse.isApi(r)) {
      view(r, s, "payment-result");
      return;
    }
    r.setAttribute("message", new PaymentService().handleCallback(params));
    view(r, s, "payment-result");
  }
}
