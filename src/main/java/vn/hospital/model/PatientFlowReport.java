package vn.hospital.model;

import java.math.BigDecimal;
import java.util.*;

public class PatientFlowReport extends ReportData {
  private Integer patientCount = 0;

  public Integer getPatientCount() {
    return patientCount;
  }

  public void setPatientCount(Integer v) {
    patientCount = v;
  }

  private Integer completedCount = 0;

  public Integer getCompletedCount() {
    return completedCount;
  }

  public void setCompletedCount(Integer v) {
    completedCount = v;
  }

  private Integer cancelledCount = 0;

  public Integer getCancelledCount() {
    return cancelledCount;
  }

  public void setCancelledCount(Integer v) {
    cancelledCount = v;
  }

  private Integer noShowCount = 0;

  public Integer getNoShowCount() {
    return noShowCount;
  }

  public void setNoShowCount(Integer v) {
    noShowCount = v;
  }

  private BigDecimal completionRate = BigDecimal.ZERO;

  public BigDecimal getCompletionRate() {
    return completionRate;
  }

  public void setCompletionRate(BigDecimal v) {
    completionRate = v;
  }

  private BigDecimal cancellationRate = BigDecimal.ZERO;

  public BigDecimal getCancellationRate() {
    return cancellationRate;
  }

  public void setCancellationRate(BigDecimal v) {
    cancellationRate = v;
  }

  private BigDecimal noShowRate = BigDecimal.ZERO;

  public BigDecimal getNoShowRate() {
    return noShowRate;
  }

  public void setNoShowRate(BigDecimal v) {
    noShowRate = v;
  }

  private List<PatientFlowRow> rows = new ArrayList<>();

  public List<PatientFlowRow> getRows() {
    return rows;
  }

  public void setRows(List<PatientFlowRow> v) {
    rows = v;
  }

  public String getTitle() {
    return "Lưu lượng bệnh nhân";
  }

  public List<Map<String, Object>> tableRows() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (var r : rows) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("Kỳ", r.getLabel());
      m.put("Lượt khám", r.getPatientCount());
      m.put("Hoàn tất", r.getCompletedCount());
      m.put("Hủy", r.getCancelledCount());
      m.put("Vắng mặt", r.getNoShowCount());
      m.put("Hoàn tất (%)", r.getCompletionRate());
      m.put("Hủy (%)", r.getCancellationRate());
      m.put("Vắng mặt (%)", r.getNoShowRate());
      result.add(m);
    }
    return result;
  }
}
