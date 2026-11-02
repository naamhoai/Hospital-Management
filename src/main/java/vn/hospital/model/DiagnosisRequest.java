package vn.hospital.model;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
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
