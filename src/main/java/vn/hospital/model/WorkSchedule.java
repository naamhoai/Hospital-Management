package vn.hospital.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
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
  private String leaveStatus;
  private String leaveReason;
  private java.time.LocalDateTime leaveRequestedAt;
  private Integer leaveReviewedBy;
  private java.time.LocalDateTime leaveReviewedAt;
  private String leaveReviewNote;

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

  public String getLeaveStatus() {
    return leaveStatus;
  }

  public void setLeaveStatus(String value) {
    leaveStatus = value;
  }

  public String getLeaveReason() {
    return leaveReason;
  }

  public void setLeaveReason(String value) {
    leaveReason = value;
  }

  public java.time.LocalDateTime getLeaveRequestedAt() {
    return leaveRequestedAt;
  }

  public void setLeaveRequestedAt(java.time.LocalDateTime value) {
    leaveRequestedAt = value;
  }

  public Integer getLeaveReviewedBy() {
    return leaveReviewedBy;
  }

  public void setLeaveReviewedBy(Integer value) {
    leaveReviewedBy = value;
  }

  public java.time.LocalDateTime getLeaveReviewedAt() {
    return leaveReviewedAt;
  }

  public void setLeaveReviewedAt(java.time.LocalDateTime value) {
    leaveReviewedAt = value;
  }

  public String getLeaveReviewNote() {
    return leaveReviewNote;
  }

  public void setLeaveReviewNote(String value) {
    leaveReviewNote = value;
  }
}
