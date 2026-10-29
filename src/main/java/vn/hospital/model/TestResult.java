package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class TestResult {
  private Integer resultId;
  private Integer orderId;
  private Integer technicianId;
  private String resultData;
  private String conclusion;
  private String fileUrl;

  public TestResult() {}

  public Integer getResultId() {
    return resultId;
  }

  public void setResultId(Integer value) {
    this.resultId = value;
  }

  public Integer getOrderId() {
    return orderId;
  }

  public void setOrderId(Integer value) {
    this.orderId = value;
  }

  public Integer getTechnicianId() {
    return technicianId;
  }

  public void setTechnicianId(Integer value) {
    this.technicianId = value;
  }

  public String getResultData() {
    return resultData;
  }

  public void setResultData(String value) {
    this.resultData = value;
  }

  public String getConclusion() {
    return conclusion;
  }

  public void setConclusion(String value) {
    this.conclusion = value;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public void setFileUrl(String value) {
    this.fileUrl = value;
  }

  private String status = "APPROVED";

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }
}
