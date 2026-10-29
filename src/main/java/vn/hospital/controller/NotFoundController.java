package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/not-found")
public class NotFoundController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setStatus(404);
    request.setAttribute("error", "Không tìm thấy trang bạn yêu cầu.");
    view(request, response, "error");
  }
}
