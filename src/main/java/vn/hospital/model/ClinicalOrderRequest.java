package vn.hospital.model;

import java.util.ArrayList;
import java.util.List;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class ClinicalOrderRequest {
  private Integer recordId;
  private List<Integer> serviceIds = new ArrayList<>();
  private String instructions;

  public ClinicalOrderRequest() {}

  public ClinicalOrderRequest(Integer recordId, List<Integer> serviceIds, String instructions) {
    this.recordId = recordId;
    this.serviceIds = serviceIds;
    this.instructions = instructions;
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer value) {
    this.recordId = value;
  }

  public List<Integer> getServiceIds() {
    return serviceIds;
  }

  public void setServiceIds(List<Integer> value) {
    this.serviceIds = value;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String value) {
    this.instructions = value;
  }
}
