package vn.hospital.model;

public class ClinicalUpdate {
  private Long id;
  private Integer queueId;
  private Integer orderId;
  private String patientName;
  private String action;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public Integer getQueueId() {
    return queueId;
  }

  public void setQueueId(Integer value) {
    queueId = value;
  }

  public Integer getOrderId() {
    return orderId;
  }

  public void setOrderId(Integer value) {
    orderId = value;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String value) {
    patientName = value;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String value) {
    action = value;
  }
}
