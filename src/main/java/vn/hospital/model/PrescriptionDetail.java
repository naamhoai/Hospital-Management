package vn.hospital.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class PrescriptionDetail {
  private Integer id;
  private Integer prescriptionId;
  private Integer medicationId;
  private Integer quantity;
  private String dosage;
  private BigDecimal unitPrice;
  private Medication medication;

  public PrescriptionDetail() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer value) {
    this.id = value;
  }

  public Integer getPrescriptionId() {
    return prescriptionId;
  }

  public void setPrescriptionId(Integer value) {
    this.prescriptionId = value;
  }

  public Integer getMedicationId() {
    return medicationId;
  }

  public void setMedicationId(Integer value) {
    this.medicationId = value;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer value) {
    this.quantity = value;
  }

  public String getDosage() {
    return dosage;
  }

  public void setDosage(String value) {
    this.dosage = value;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal value) {
    this.unitPrice = value;
  }

  public Medication getMedication() {
    return medication;
  }

  public void setMedication(Medication value) {
    this.medication = value;
  }
}
