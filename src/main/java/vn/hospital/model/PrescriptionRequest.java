package vn.hospital.model;

import java.util.ArrayList;
import java.util.List;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class PrescriptionRequest {
  private Integer recordId;
  private List<PrescriptionDetail> items = new ArrayList<>();
  private String advice;
  private Boolean overrideInteractions;

  public PrescriptionRequest() {}

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer value) {
    this.recordId = value;
  }

  public List<PrescriptionDetail> getItems() {
    return items;
  }

  public void setItems(List<PrescriptionDetail> value) {
    this.items = value;
  }

  public String getAdvice() {
    return advice;
  }

  public void setAdvice(String value) {
    this.advice = value;
  }

  public Boolean getOverrideInteractions() {
    return overrideInteractions;
  }

  public void setOverrideInteractions(Boolean value) {
    this.overrideInteractions = value;
  }
}
