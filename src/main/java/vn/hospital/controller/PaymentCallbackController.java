package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import vn.hospital.service.PaymentService;
import vn.hospital.util.BusinessException;

@WebServlet(urlPatterns = {"/payment/return", "/payment/ipn"})
public class PaymentCallbackController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Map<String, String> params = new HashMap<>();
    request
        .getParameterMap()
        .forEach(
            (k, v) -> {
              if (v.length == 1) params.put(k, v[0]);
            });
    if (request.getServletPath().endsWith("/ipn")) {
      response.setContentType("application/json");
      try {
        new PaymentService().handleCallback(params);
        response.getWriter().write("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
      } catch (BusinessException e) {
        response.getWriter().write("{\"RspCode\":\"99\",\"Message\":\"Validation failed\"}");
      }
      return;
    }
    request.setAttribute("message", new PaymentService().handleCallback(params));
    view(request, response, "payment-result");
  }
}
