package vn.hospital.model;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class Department {
  private Integer departmentId;
  private String name;
  private String description;
  private String status;

  public Department() {}

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    this.description = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
