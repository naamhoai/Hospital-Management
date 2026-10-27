package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class DiagnosisRequest {
  private String symptoms;
  private String icdCode;
  private String diagnosis;
  private String notes;

  public DiagnosisRequest() {}

  public DiagnosisRequest(String symptoms, String icdCode, String diagnosis, String notes) {
    this.symptoms = symptoms;
    this.icdCode = icdCode;
    this.diagnosis = diagnosis;
    this.notes = notes;
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
}
