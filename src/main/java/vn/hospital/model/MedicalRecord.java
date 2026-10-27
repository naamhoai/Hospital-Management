package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class MedicalRecord {
  private Integer recordId;
  private Integer appointmentId;
  private Integer patientId;
  private Integer doctorId;
  private String symptoms;
  private String icdCode;
  private String diagnosis;
  private String notes;
  private String status;

  public MedicalRecord() {}

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer value) {
    this.recordId = value;
  }

  public Integer getAppointmentId() {
    return appointmentId;
  }

  public void setAppointmentId(Integer value) {
    this.appointmentId = value;
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

  public String getSymptoms() {
    return symptoms;
  }

  public void setSymptoms(String value) {
    this.symptoms = value;
  }

  public String getIcdCode() {
    return icdCode;
  }

  public void setIcdCode(String value) {
    this.icdCode = value;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public void setDiagnosis(String value) {
    this.diagnosis = value;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String value) {
    this.notes = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
