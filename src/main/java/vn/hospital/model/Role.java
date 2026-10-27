package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class Role {
  private Integer roleId;
  private String name;
  private String description;

  public Role() {}

  public Integer getRoleId() {
    return roleId;
  }

  public void setRoleId(Integer value) {
    this.roleId = value;
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
}
