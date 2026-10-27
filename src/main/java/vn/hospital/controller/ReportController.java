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
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    var filter = new ReportFilter();
    filter.setFromDate(
        p(r, "from").isBlank() ? LocalDate.now().withDayOfMonth(1) : Checks.date(p(r, "from")));
    filter.setToDate(p(r, "to").isBlank() ? LocalDate.now() : Checks.date(p(r, "to")));
    filter.setDepartmentId(
        p(r, "departmentId").isBlank() ? null : Actors.id(id(r, "departmentId")));
    filter.setServiceId(p(r, "serviceId").isBlank() ? null : Actors.id(id(r, "serviceId")));
    filter.setPaymentMethod(p(r, "method"));
    filter.setServiceType(p(r, "serviceType"));
    filter.setGroupBy(p(r, "groupBy").isBlank() ? "DAY" : p(r, "groupBy"));
    String type = p(r, "type").isBlank() ? "flow" : p(r, "type");
    if (type.equals("flow")) filter.setServiceType("");
    var service = new AnalyticsService(user(r));
    String format = p(r, "format");
    if (!format.isBlank()) {
      download(
          s,
          service.exportReport(type, filter, format),
          format.equals("pdf")
              ? "application/pdf"
              : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "hospital-" + type + "." + format);
      return;
    }
    var report = service.generateReport(type, filter);
    r.setAttribute("report", report);
    r.setAttribute("rows", report.tableRows());
    r.setAttribute("filter", filter);
    r.setAttribute("type", type);
    service.filterOptions().forEach(r::setAttribute);
    view(r, s, "reports");
  }
}
