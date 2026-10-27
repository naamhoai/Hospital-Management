package vn.hospital.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class PatientFlowRow {
  private String label;
  private Integer patientCount;
  private Integer completedCount;
  private Integer cancelledCount;
  private Integer noShowCount;
  private BigDecimal completionRate;
  private BigDecimal cancellationRate;
  private BigDecimal noShowRate;

  public PatientFlowRow() {}

  public String getLabel() {
    return label;
  }

  public void setLabel(String value) {
    this.label = value;
  }

  public Integer getPatientCount() {
    return patientCount;
  }

  public void setPatientCount(Integer value) {
    this.patientCount = value;
  }

  public Integer getCompletedCount() {
    return completedCount;
  }

  public void setCompletedCount(Integer value) {
    this.completedCount = value;
  }

  public Integer getCancelledCount() {
    return cancelledCount;
  }

  public void setCancelledCount(Integer value) {
    this.cancelledCount = value;
  }

  public Integer getNoShowCount() {
    return noShowCount;
  }

  public void setNoShowCount(Integer value) {
    this.noShowCount = value;
  }

  public BigDecimal getCompletionRate() {
    return completionRate;
  }

  public void setCompletionRate(BigDecimal value) {
    this.completionRate = value;
  }

  public BigDecimal getCancellationRate() {
    return cancellationRate;
  }

  public void setCancellationRate(BigDecimal value) {
    this.cancellationRate = value;
  }

  public BigDecimal getNoShowRate() {
    return noShowRate;
  }

  public void setNoShowRate(BigDecimal value) {
    this.noShowRate = value;
  }
}
