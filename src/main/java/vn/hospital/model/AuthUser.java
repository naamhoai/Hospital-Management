package vn.hospital.model;

import java.io.Serializable;

public class AuthUser implements Serializable {
  private final long userId;
  private final String fullName;
  private final String role;

  public AuthUser(long userId, String fullName, String role) {
    this.userId = userId;
    this.fullName = fullName;
    this.role = role;
  }

  public long getUserId() {
    return userId;
  }

  public String getFullName() {
    return fullName;
  }

  public String getRole() {
    return role;
  }

  public boolean is(String... roles) {
    for (String r : roles) if (r.equals(role)) return true;
    return false;
  }
}
