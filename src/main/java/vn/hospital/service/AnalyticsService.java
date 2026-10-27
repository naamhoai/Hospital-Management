package vn.hospital.service;

import java.io.File;
import java.math.*;
import java.util.*;
import vn.hospital.dao.ReportDAO;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class AnalyticsService {
  private final AuthUser actor;

  public AnalyticsService() {
    this(null);
  }

  public AnalyticsService(AuthUser actor) {
    this.actor = actor;
  }

  public void validateFilter(ReportFilter f) {
    Checks.require(
        f != null
            && f.getFromDate() != null
            && f.getToDate() != null
            && !f.getToDate().isBefore(f.getFromDate())
            && !f.getToDate().isAfter(f.getFromDate().plusYears(2)),
        "Khoảng thời gian phải hợp lệ và tối đa 2 năm.");
    if (f.getGroupBy() == null || f.getGroupBy().isBlank()) f.setGroupBy("DAY");
    Checks.require(
        Set.of("DAY", "MONTH", "QUARTER").contains(f.getGroupBy()),
        "Kiểu nhóm thời gian không hợp lệ.");
    if (f.getServiceType() == null) f.setServiceType("");
    Checks.require(
        Set.of("", "CONSULTATION", "ORDER", "MEDICATION").contains(f.getServiceType()),
        "Loại chi phí không hợp lệ.");
    Checks.require(
        f.getServiceId() == null
            || f.getServiceType().isBlank()
            || f.getServiceType().equals("ORDER"),
        "Mã dịch vụ chỉ định chỉ dùng với loại cận lâm sàng.");
    if (f.getPaymentMethod() == null) f.setPaymentMethod("");
    Checks.require(
        Set.of("", "CASH", "VNPAY").contains(f.getPaymentMethod()),
        "Phương thức thanh toán không hợp lệ.");
    Checks.require(f.getDepartmentId() == null || f.getDepartmentId() > 0, "Mã khoa không hợp lệ.");
    Checks.require(f.getServiceId() == null || f.getServiceId() > 0, "Mã dịch vụ không hợp lệ.");
  }

  public ReportData generateReport(String type, ReportFilter filter) {
    Actors.require(actor, "MANAGER", "ADMIN");
    validateFilter(filter);
    return switch (type) {
      case "flow" -> getPatientFlowReport(filter);
      case "revenue" -> getRevenueReport(filter);
      default -> throw new BusinessException("Loại báo cáo không hợp lệ.");
    };
  }

  public PatientFlowReport getPatientFlowReport(ReportFilter filter) {
    Actors.require(actor, "MANAGER", "ADMIN");
    validateFilter(filter);
    PatientFlowReport report = new ReportDAO().queryPatientFlowMetrics(filter);
    report.setCompletionRate(rate(report.getCompletedCount(), report.getPatientCount()));
    report.setCancellationRate(rate(report.getCancelledCount(), report.getPatientCount()));
    report.setNoShowRate(rate(report.getNoShowCount(), report.getPatientCount()));
    List<ChartDataPoint> series = new ArrayList<>();
    for (var row : report.getRows()) {
      row.setCompletionRate(rate(row.getCompletedCount(), row.getPatientCount()));
      row.setCancellationRate(rate(row.getCancelledCount(), row.getPatientCount()));
      row.setNoShowRate(rate(row.getNoShowCount(), row.getPatientCount()));
      series.add(new ChartDataPoint(row.getLabel(), BigDecimal.valueOf(row.getPatientCount())));
    }
    report.setFilter(filter);
    report.setSeries(series);
    return report;
  }

  public RevenueReport getRevenueReport(ReportFilter filter) {
    Actors.require(actor, "MANAGER", "ADMIN");
    validateFilter(filter);
    RevenueReport report = new ReportDAO().queryRevenueMetrics(filter);
    report.setFilter(filter);
    Map<String, BigDecimal> totals = new LinkedHashMap<>();
    for (var row : report.getRows())
      totals.merge(row.getLabel(), row.getTotalRevenue(), BigDecimal::add);
    report.setSeries(
        totals.entrySet().stream().map(e -> new ChartDataPoint(e.getKey(), e.getValue())).toList());
    return report;
  }

  private BigDecimal rate(Integer count, Integer total) {
    return total == null || total == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(count)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  public File exportReport(String type, ReportFilter filter, String format) {
    ReportData report = generateReport(type, filter);
    return switch (format.toLowerCase(Locale.ROOT)) {
      case "pdf" -> new ReportExportService().exportPDF(report);
      case "xlsx" -> new ReportExportService().exportExcel(report);
      default -> throw new BusinessException("Chỉ hỗ trợ PDF hoặc XLSX.");
    };
  }

  public Map<String, Object> filterOptions() {
    Actors.require(actor, "MANAGER", "ADMIN");
    var dao = new ReportDAO();
    return Map.of("departments", dao.filterDepartments(), "services", dao.filterServices());
  }

  public List<Map<String, Object>> generateReport(
      String type, String from, String to, String method) {
    Actors.require(actor, "MANAGER", "ADMIN");
    var f = new ReportFilter();
    f.setFromDate(Checks.date(from));
    f.setToDate(Checks.date(to));
    f.setPaymentMethod(method);
    f.setGroupBy("DAY");
    validateFilter(f);
    if (type.equals("revenue"))
      return new ReportDAO().queryRevenueMetrics(f.getFromDate(), f.getToDate(), method);
    if (type.equals("flow")) return new ReportDAO().queryPatientFlowMetrics(f).tableRows();
    throw new BusinessException("Loại báo cáo không hợp lệ.");
  }
}
