package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class WorkSchedule {
  private Integer scheduleId;
  private Integer doctorId;
  private LocalDate workDate;
  private String shift;
  private LocalTime startTime;
  private LocalTime endTime;
  private Integer maxSlots;
  private Integer bookedCount;
  private String status;

  public WorkSchedule() {}

  public Integer getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Integer value) {
    this.scheduleId = value;
  }

  public Integer getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Integer value) {
    this.doctorId = value;
  }

  public LocalDate getWorkDate() {
    return workDate;
  }

  public void setWorkDate(LocalDate value) {
    this.workDate = value;
  }

  public String getShift() {
    return shift;
  }

  public void setShift(String value) {
    this.shift = value;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime value) {
    this.startTime = value;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime value) {
    this.endTime = value;
  }

  public Integer getMaxSlots() {
    return maxSlots;
  }

  public void setMaxSlots(Integer value) {
    this.maxSlots = value;
  }

  public Integer getBookedCount() {
    return bookedCount;
  }

  public void setBookedCount(Integer value) {
    this.bookedCount = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
