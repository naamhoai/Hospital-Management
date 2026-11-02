package vn.hospital.model;

import java.math.BigDecimal;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class MedicalService {
  private Integer serviceId;
  private Integer departmentId;
  private String name;
  private String category;
  private BigDecimal unitPrice;
  private String status;

  public MedicalService() {}

  public Integer getServiceId() {
    return serviceId;
  }

  public void setServiceId(Integer value) {
    this.serviceId = value;
  }

  public Integer getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Integer value) {
    this.departmentId = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String value) {
    this.category = value;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal value) {
    this.unitPrice = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
