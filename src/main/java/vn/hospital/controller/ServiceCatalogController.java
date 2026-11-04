package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.ServiceCatalogService;
import vn.hospital.util.Checks;

@WebServlet("/services")
public class ServiceCatalogController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setAttribute("services", new ServiceCatalogService().findCatalogServices());
    request.setAttribute("departments", new ServiceCatalogService().findDepartments());
    request.setAttribute("componentOptions", new ServiceCatalogService().componentOptions());
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
            param(request, "description"),
            param(request, "category"),
            Checks.money(param(request, "price")),
            param(request, "status"),
            request.getParameterValues("componentId") == null
                ? java.util.List.of()
                : java.util.Arrays.stream(request.getParameterValues("componentId"))
                    .map(value -> vn.hospital.util.Actors.id(Checks.id(value)))
                    .toList());
    redirect(
        request, response, "/services", "Đã lưu dịch vụ. Giá mới áp dụng cho phát sinh tiếp theo.");
  }
}
