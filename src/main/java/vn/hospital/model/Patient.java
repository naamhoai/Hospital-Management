package vn.hospital.model;

import java.time.LocalDate;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class Patient {
  private Integer patientId;
  private String identityCard;
  private String insuranceNumber;
  private LocalDate dateOfBirth;
  private String address;
  private String emergencyContact;

  public Patient() {}

  public Integer getPatientId() {
    return patientId;
  }

  public void setPatientId(Integer value) {
    this.patientId = value;
  }

  public String getIdentityCard() {
    return identityCard;
  }

  public void setIdentityCard(String value) {
    this.identityCard = value;
  }

  public String getInsuranceNumber() {
    return insuranceNumber;
  }

  public void setInsuranceNumber(String value) {
    this.insuranceNumber = value;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(LocalDate value) {
    this.dateOfBirth = value;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String value) {
    this.address = value;
  }

  public String getEmergencyContact() {
    return emergencyContact;
  }

  public void setEmergencyContact(String value) {
    this.emergencyContact = value;
  }
}
