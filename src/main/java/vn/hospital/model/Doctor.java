package vn.hospital.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class Doctor {
  private Integer doctorId;
  private Integer departmentId;
  private String roomNumber;
  private String academicTitle;
  private Integer experienceYears;
  private BigDecimal ratingAverage;

  public Doctor() {}

  public Integer getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Integer value) {
    this.doctorId = value;
  }

  public Integer getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Integer value) {
    this.departmentId = value;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String value) {
    this.roomNumber = value;
  }

  public String getAcademicTitle() {
    return academicTitle;
  }

  public void setAcademicTitle(String value) {
    this.academicTitle = value;
  }

  public Integer getExperienceYears() {
    return experienceYears;
  }

  public void setExperienceYears(Integer value) {
    this.experienceYears = value;
  }

  public BigDecimal getRatingAverage() {
    return ratingAverage;
  }

  public void setRatingAverage(BigDecimal value) {
    this.ratingAverage = value;
  }
}
