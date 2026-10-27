package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.model.*;
import vn.hospital.util.Database;

public class MedicationDAO {
  public List<Medication> searchApproved(String keyword) {
    return Database.tx(c -> searchApproved(c, keyword));
  }

  public List<Medication> searchApproved(Connection c, String keyword) throws SQLException {
    return BeanMapper.list(
        Jdbc.list(
            c,
            "SELECT * FROM medication WHERE status='ACTIVE' AND (name LIKE ? OR active_ingredient"
                + " LIKE ?) ORDER BY name",
            "%" + keyword + "%",
            "%" + keyword + "%"),
        Medication.class);
  }

  public Medication findById(Integer id) {
    return Database.tx(c -> findById(c, id));
  }

  public Medication findById(Connection c, long id) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(
            c, "SELECT * FROM medication WHERE medication_id=? AND status='ACTIVE' FOR UPDATE", id),
        Medication.class);
  }

  public Boolean checkStock(List<PrescriptionDetail> items) {
    return Database.tx(c -> checkStock(c, items));
  }

  public boolean checkStock(Connection c, List<PrescriptionDetail> items) throws SQLException {
    for (var item : items) {
      Medication med = findById(c, item.getMedicationId());
      if (med == null || med.getStockQuantity() < item.getQuantity()) return false;
    }
    return true;
  }

  public boolean deductStock(Connection c, PrescriptionDetail item) throws SQLException {
    return Jdbc.update(
            c,
            "UPDATE medication SET stock_quantity=stock_quantity-? WHERE medication_id=? AND"
                + " stock_quantity>=?",
            item.getQuantity(),
            item.getMedicationId(),
            item.getQuantity())
        == 1;
  }
}
