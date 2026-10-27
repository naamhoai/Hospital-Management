package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class Appointment {
  private Integer appointmentId;
  private String appointmentCode;
  private Integer patientId;
  private Integer doctorId;
  private Integer scheduleId;
  private LocalDate appointmentDate;
  private LocalTime appointmentTime;
  private String initialSymptoms;
  private Integer rescheduleCount;
  private String status;

  public Appointment() {}

  public Integer getAppointmentId() {
    return appointmentId;
  }

  public void setAppointmentId(Integer value) {
    this.appointmentId = value;
  }

  public String getAppointmentCode() {
    return appointmentCode;
  }

  public void setAppointmentCode(String value) {
    this.appointmentCode = value;
  }

  public Integer getPatientId() {
    return patientId;
  }

  public void setPatientId(Integer value) {
    this.patientId = value;
  }

  public Integer getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Integer value) {
    this.doctorId = value;
  }

  public Integer getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Integer value) {
    this.scheduleId = value;
  }

  public LocalDate getAppointmentDate() {
    return appointmentDate;
  }

  public void setAppointmentDate(LocalDate value) {
    this.appointmentDate = value;
  }

  public LocalTime getAppointmentTime() {
    return appointmentTime;
  }

  public void setAppointmentTime(LocalTime value) {
    this.appointmentTime = value;
  }

  public String getInitialSymptoms() {
    return initialSymptoms;
  }

  public void setInitialSymptoms(String value) {
    this.initialSymptoms = value;
  }

  public Integer getRescheduleCount() {
    return rescheduleCount;
  }

  public void setRescheduleCount(Integer value) {
    this.rescheduleCount = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
