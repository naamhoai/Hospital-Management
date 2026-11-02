package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.service.ServiceCatalogService;
import vn.hospital.util.Checks;

@WebServlet("/services")
public class ServiceCatalogController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setAttribute("services", new ServiceCatalogService().findCatalogServices());
    request.setAttribute("departments", new ServiceCatalogService().findDepartments());
    view(request, response, "services");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    new ServiceCatalogService()
        .save(
            user(request),
            param(request, "serviceId").isBlank() ? 0 : id(request, "serviceId"),
            id(request, "departmentId"),
            param(request, "name"),
            param(request, "category"),
            Checks.money(param(request, "price")),
            param(request, "status"));
    redirect(
        request, response, "/services", "Đã lưu dịch vụ. Giá mới áp dụng cho phát sinh tiếp theo.");
  }
}
