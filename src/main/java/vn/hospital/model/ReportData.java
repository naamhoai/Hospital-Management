package vn.hospital.model;

import java.time.*;
import java.util.*;

public abstract class ReportData {
  private LocalDate fromDate, toDate;
  private LocalDateTime generatedAt = LocalDateTime.now();
  private List<ChartDataPoint> series = new ArrayList<>();

  public LocalDate getFromDate() {
    return fromDate;
  }

  public void setFromDate(LocalDate v) {
    fromDate = v;
  }

  public LocalDate getToDate() {
    return toDate;
  }

  public void setToDate(LocalDate v) {
    toDate = v;
  }

  public LocalDateTime getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(LocalDateTime v) {
    generatedAt = v;
  }

  public List<ChartDataPoint> getSeries() {
    return series;
  }

  public void setSeries(List<ChartDataPoint> v) {
    series = v;
  }

  private ReportFilter filter;

  public ReportFilter getFilter() {
    return filter;
  }

  public void setFilter(ReportFilter value) {
    filter = value;
  }

  public abstract String getTitle();

  public abstract List<Map<String, Object>> tableRows();
}
