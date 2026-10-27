package vn.hospital.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class RevenueRow {
  private String label;
  private String department;
  private String service;
  private String paymentMethod;
  private Integer invoiceCount;
  private BigDecimal totalRevenue;

  public RevenueRow() {}

  public String getLabel() {
    return label;
  }

  public void setLabel(String value) {
    this.label = value;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String value) {
    this.department = value;
  }

  public String getService() {
    return service;
  }

  public void setService(String value) {
    this.service = value;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String value) {
    this.paymentMethod = value;
  }

  public Integer getInvoiceCount() {
    return invoiceCount;
  }

  public void setInvoiceCount(Integer value) {
    this.invoiceCount = value;
  }

  public BigDecimal getTotalRevenue() {
    return totalRevenue;
  }

  public void setTotalRevenue(BigDecimal value) {
    this.totalRevenue = value;
  }
}
