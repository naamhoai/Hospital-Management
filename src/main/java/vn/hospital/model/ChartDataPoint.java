package vn.hospital.model;

import java.math.BigDecimal;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class ChartDataPoint {
  private String label;
  private BigDecimal value;

  public ChartDataPoint() {}

  public ChartDataPoint(String label, BigDecimal value) {
    this.label = label;
    this.value = value;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String value) {
    this.label = value;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }
}
