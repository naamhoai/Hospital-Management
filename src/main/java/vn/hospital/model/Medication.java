package vn.hospital.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class Medication {
  private Integer medicationId;
  private String name;
  private String activeIngredient;
  private String dosageForm;
  private String unit;
  private BigDecimal unitPrice;
  private Integer stockQuantity;

  public Medication() {}

  public Integer getMedicationId() {
    return medicationId;
  }

  public void setMedicationId(Integer value) {
    this.medicationId = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getActiveIngredient() {
    return activeIngredient;
  }

  public void setActiveIngredient(String value) {
    this.activeIngredient = value;
  }

  public String getDosageForm() {
    return dosageForm;
  }

  public void setDosageForm(String value) {
    this.dosageForm = value;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String value) {
    this.unit = value;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal value) {
    this.unitPrice = value;
  }

  public Integer getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(Integer value) {
    this.stockQuantity = value;
  }
}
