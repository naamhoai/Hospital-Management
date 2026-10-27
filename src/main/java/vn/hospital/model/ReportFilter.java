package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class ReportFilter {
  private LocalDate fromDate;
  private LocalDate toDate;
  private Integer departmentId;
  private Integer serviceId;
  private String paymentMethod;
  private String groupBy;

  public ReportFilter() {}

  public LocalDate getFromDate() {
    return fromDate;
  }

  public void setFromDate(LocalDate value) {
    this.fromDate = value;
  }

  public LocalDate getToDate() {
    return toDate;
  }

  public void setToDate(LocalDate value) {
    this.toDate = value;
  }

  public Integer getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Integer value) {
    this.departmentId = value;
  }

  public Integer getServiceId() {
    return serviceId;
  }

  public void setServiceId(Integer value) {
    this.serviceId = value;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String value) {
    this.paymentMethod = value;
  }

  public String getGroupBy() {
    return groupBy;
  }

  public void setGroupBy(String value) {
    this.groupBy = value;
  }

  private String serviceType = "";

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String value) {
    serviceType = value;
  }
}
