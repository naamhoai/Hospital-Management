package vn.hospital.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class Invoice {
  private Integer invoiceId;
  private String invoiceCode;
  private Integer recordId;
  private Integer patientId;
  private Integer cashierId;
  private BigDecimal totalAmount;
  private String paymentMethod;
  private String transactionId;
  private String status;

  public Invoice() {}

  public Integer getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(Integer value) {
    this.invoiceId = value;
  }

  public String getInvoiceCode() {
    return invoiceCode;
  }

  public void setInvoiceCode(String value) {
    this.invoiceCode = value;
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer value) {
    this.recordId = value;
  }

  public Integer getPatientId() {
    return patientId;
  }

  public void setPatientId(Integer value) {
    this.patientId = value;
  }

  public Integer getCashierId() {
    return cashierId;
  }

  public void setCashierId(Integer value) {
    this.cashierId = value;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal value) {
    this.totalAmount = value;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String value) {
    this.paymentMethod = value;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String value) {
    this.transactionId = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
