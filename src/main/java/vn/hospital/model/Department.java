package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
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
