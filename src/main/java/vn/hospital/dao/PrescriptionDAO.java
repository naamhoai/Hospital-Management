package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class PrescriptionDAO {
  public Prescription findById(Integer id) {
    return Database.tx(c -> findById(c, id));
  }

  public Prescription findById(Connection c, long id) throws SQLException {
    Prescription rx =
        BeanMapper.map(
            Jdbc.one(c, "SELECT * FROM prescription WHERE prescription_id=?", id),
            Prescription.class);
    if (rx != null) {
      List<PrescriptionDetail> items =
          BeanMapper.list(
              Jdbc.list(
                  c, "SELECT * FROM prescription_detail WHERE prescription_id=? ORDER BY id", id),
              PrescriptionDetail.class);
      for (var item : items)
        item.setMedication(
            BeanMapper.map(
                Jdbc.one(
                    c, "SELECT * FROM medication WHERE medication_id=?", item.getMedicationId()),
                Medication.class));
      rx.setItems(items);
    }
    return rx;
  }

  public List<Prescription> findByRecordId(Integer id) {
    return Database.tx(c -> findByRecordId(c, id));
  }

  public List<Prescription> findByRecordId(Connection c, long id) throws SQLException {
    return BeanMapper.list(
        Jdbc.list(
            c, "SELECT * FROM prescription WHERE record_id=? ORDER BY prescription_id DESC", id),
        Prescription.class);
  }

  public Prescription saveWithDetails(Prescription rx, List<PrescriptionDetail> items) {
    return Database.tx(c -> saveWithDetails(c, rx, items));
  }

  public Prescription saveWithDetails(Connection c, Prescription rx, List<PrescriptionDetail> items)
      throws SQLException {
    rx.setPrescriptionId(
        Actors.id(
            Jdbc.insert(
                c,
                "INSERT INTO prescription(record_id,advice) VALUES(?,?)",
                rx.getRecordId(),
                rx.getAdvice())));
    for (var item : items) {
      item.setPrescriptionId(rx.getPrescriptionId());
      item.setId(
          Actors.id(
              Jdbc.insert(
                  c,
                  "INSERT INTO"
                      + " prescription_detail(prescription_id,medication_id,quantity,dosage,unit_price)"
                      + " VALUES(?,?,?,?,?)",
                  rx.getPrescriptionId(),
                  item.getMedicationId(),
                  item.getQuantity(),
                  item.getDosage(),
                  item.getUnitPrice())));
    }
    return findById(c, rx.getPrescriptionId());
  }

  public int markPaidByInvoice(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE prescription p SET payment_status='PAID' WHERE p.prescription_id IN (SELECT"
            + " pd.prescription_id FROM prescription_detail pd JOIN invoice_line l ON"
            + " l.source_type='MEDICATION' AND l.source_id=pd.id WHERE l.invoice_id=?)",
        values);
  }
}
