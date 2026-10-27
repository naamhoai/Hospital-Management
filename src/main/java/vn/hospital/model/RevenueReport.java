package vn.hospital.model;

import java.math.BigDecimal;
import java.util.*;

public class RevenueReport extends ReportData {
  private Integer paidInvoiceCount = 0;

  public Integer getPaidInvoiceCount() {
    return paidInvoiceCount;
  }

  public void setPaidInvoiceCount(Integer v) {
    paidInvoiceCount = v;
  }

  private BigDecimal totalRevenue = BigDecimal.ZERO;

  public BigDecimal getTotalRevenue() {
    return totalRevenue;
  }

  public void setTotalRevenue(BigDecimal v) {
    totalRevenue = v;
  }

  private String department = null;

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String v) {
    department = v;
  }

  private String service = null;

  public String getService() {
    return service;
  }

  public void setService(String v) {
    service = v;
  }

  private String paymentMethod = null;

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String v) {
    paymentMethod = v;
  }

  private List<RevenueRow> rows = new ArrayList<>();

  public List<RevenueRow> getRows() {
    return rows;
  }

  public void setRows(List<RevenueRow> v) {
    rows = v;
  }

  public String getTitle() {
    return "Doanh thu dịch vụ";
  }

  public List<Map<String, Object>> tableRows() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (var r : rows) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("Kỳ", r.getLabel());
      m.put("Chuyên khoa", r.getDepartment());
      m.put("Dịch vụ", r.getService());
      m.put("Phương thức", r.getPaymentMethod());
      m.put("Số hóa đơn", r.getInvoiceCount());
      m.put("Doanh thu (VND)", r.getTotalRevenue());
      result.add(m);
    }
    return result;
  }
}
