package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import vn.hospital.model.ReportFilter;
import vn.hospital.service.AnalyticsService;
import vn.hospital.util.*;

@WebServlet("/reports")
public class ReportController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    var filter = new ReportFilter();
    filter.setFromDate(
        param(request, "from").isBlank()
            ? LocalDate.now().withDayOfMonth(1)
            : Checks.date(param(request, "from")));
    filter.setToDate(
        param(request, "to").isBlank() ? LocalDate.now() : Checks.date(param(request, "to")));
    filter.setDepartmentId(
        param(request, "departmentId").isBlank() ? null : Actors.id(id(request, "departmentId")));
    filter.setServiceId(
        param(request, "serviceId").isBlank() ? null : Actors.id(id(request, "serviceId")));
    filter.setPaymentMethod(param(request, "method"));
    filter.setServiceType(param(request, "serviceType"));
    filter.setGroupBy(param(request, "groupBy").isBlank() ? "DAY" : param(request, "groupBy"));
    String type = param(request, "type").isBlank() ? "flow" : param(request, "type");
    if (type.equals("flow")) filter.setServiceType("");
    var service = new AnalyticsService(user(request));
    String format = param(request, "format");
    if (!format.isBlank()) {
      download(
          response,
          service.exportReport(type, filter, format),
          format.equals("pdf")
              ? "application/pdf"
              : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "hospital-" + type + "." + format);
      return;
    }
    var report = service.generateReport(type, filter);
    request.setAttribute("report", report);
    request.setAttribute("rows", report.tableRows());
    request.setAttribute("filter", filter);
    request.setAttribute("type", type);
    service.filterOptions().forEach(request::setAttribute);
    view(request, response, "reports");
  }
}
