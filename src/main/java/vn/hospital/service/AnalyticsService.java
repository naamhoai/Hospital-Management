package vn.hospital.service;

import java.io.File;
import java.math.*;
import java.util.*;
import vn.hospital.dao.ReportDAO;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class AnalyticsService {
  private final AuthUser actor;

  public AnalyticsService(AuthUser actor) {
    this.actor = actor;
  }

  /** Giới hạn khoảng ngày và chỉ chấp nhận các kiểu nhóm/bộ lọc đã định nghĩa. */
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

  /**
   * SQ20/WF04 - Lập báo cáo cho quản lý. Đầu vào: type flow/revenue và ReportFilter gồm ngày, khoa,
   * dịch vụ, phương thức, cách nhóm. Kiểm tra quyền và bộ lọc, chọn truy vấn đọc của ReportDAO. Đầu
   * ra: PatientFlowReport hoặc RevenueReport cho bảng/biểu đồ; dữ liệu rỗng vẫn là kết quả hợp lệ.
   */
  public ReportData generateReport(String type, ReportFilter filter) {
    Actors.require(actor, "MANAGER", "ADMIN");
    validateFilter(filter);
    return switch (type) {
      case "flow" -> getPatientFlowReport(filter);
      case "revenue" -> getRevenueReport(filter);
      default -> throw new BusinessException("Loại báo cáo không hợp lệ.");
    };
  }

  /**
   * SQ20 - Thống kê lượt khám. Nhận bộ lọc, đọc số lượt/hoàn tất/hủy/vắng rồi tính tỷ lệ và chuỗi
   * biểu đồ. Trả PatientFlowReport; mẫu số bằng 0 trả tỷ lệ 0, không sửa các lịch hẹn.
   */
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

  /**
   * SQ20 - Thống kê doanh thu. Nhận bộ lọc thời gian/khoa/dịch vụ/phương thức; chỉ cộng invoice
   * PAID từ các dòng phí. Trả RevenueReport và chuỗi biểu đồ; không tính hóa đơn UNPAID vào doanh
   * thu.
   */
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

  /** Mẫu số bằng 0 trả về 0%; dùng BigDecimal để tính tỷ lệ có kiểm soát làm tròn. */
  private BigDecimal rate(Integer count, Integer total) {
    return total == null || total == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(count)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  /**
   * SQ20 - Xuất báo cáo đang xem. Đầu vào: cùng type/filter với màn hình và format pdf/xlsx. Lập
   * lại dữ liệu bằng cùng bộ lọc, trả file tạm để Controller gửi về trình duyệt. Không ghi hoặc
   * thay đổi bệnh án/hóa đơn trong quá trình xuất.
   */
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
}
