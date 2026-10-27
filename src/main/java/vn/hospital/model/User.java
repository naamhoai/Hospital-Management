package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class User {
  private Integer userId;
  private Integer roleId;
  private String email;
  private String password;
  private String fullName;
  private String phoneNumber;
  private String gender;
  private String status;

  public User() {}

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer value) {
    this.userId = value;
  }

  public Integer getRoleId() {
    return roleId;
  }

  public void setRoleId(Integer value) {
    this.roleId = value;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String value) {
    this.email = value;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String value) {
    this.password = value;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String value) {
    this.fullName = value;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String value) {
    this.phoneNumber = value;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String value) {
    this.gender = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
