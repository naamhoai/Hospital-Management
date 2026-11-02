package vn.hospital.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class Prescription {
  private Integer prescriptionId;
  private Integer recordId;
  private LocalDateTime prescribedDate;
  private String advice;
  private String paymentStatus;
  private List<PrescriptionDetail> items = new ArrayList<>();

  public Prescription() {}

  public Integer getPrescriptionId() {
    return prescriptionId;
  }

  public void setPrescriptionId(Integer value) {
    this.prescriptionId = value;
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer value) {
    this.recordId = value;
  }

  public LocalDateTime getPrescribedDate() {
    return prescribedDate;
  }

  public void setPrescribedDate(LocalDateTime value) {
    this.prescribedDate = value;
  }

  public String getAdvice() {
    return advice;
  }

  public void setAdvice(String value) {
    this.advice = value;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(String value) {
    this.paymentStatus = value;
  }

  public List<PrescriptionDetail> getItems() {
    return items;
  }

  public void setItems(List<PrescriptionDetail> value) {
    this.items = value;
  }
}
