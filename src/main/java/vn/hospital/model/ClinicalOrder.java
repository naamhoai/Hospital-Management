package vn.hospital.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class ClinicalOrder {
  private Integer orderId;
  private Integer recordId;
  private Integer serviceId;
  private LocalDateTime orderDate;
  private String instructions;
  private String paymentStatus;
  private String status;

  public ClinicalOrder() {}

  public Integer getOrderId() {
    return orderId;
  }

  public void setOrderId(Integer value) {
    this.orderId = value;
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer value) {
    this.recordId = value;
  }

  public Integer getServiceId() {
    return serviceId;
  }

  public void setServiceId(Integer value) {
    this.serviceId = value;
  }

  public LocalDateTime getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(LocalDateTime value) {
    this.orderDate = value;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String value) {
    this.instructions = value;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(String value) {
    this.paymentStatus = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }

  private MedicalService service;
  private TestResult result;
  private BigDecimal unitPrice;

  public MedicalService getService() {
    return service;
  }

  public void setService(MedicalService v) {
    service = v;
  }

  public TestResult getResult() {
    return result;
  }

  public void setResult(TestResult v) {
    result = v;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal v) {
    unitPrice = v;
  }
}
