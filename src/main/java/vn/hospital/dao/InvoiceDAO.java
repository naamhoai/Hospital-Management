package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class InvoiceDAO {
  public Map<String, Object> findById(Connection c, long id) throws SQLException {
    return Jdbc.one(c, "SELECT * FROM invoice WHERE invoice_id=? FOR UPDATE", id);
  }

  public List<Map<String, Object>> list() {
    return Jdbc.read(
        "SELECT i.*,u.full_name patient_name FROM invoice i JOIN users u ON u.user_id=i.patient_id"
            + " ORDER BY invoice_id DESC");
  }

  public Map<String, Object> findPatientRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(c, "SELECT patient_id FROM invoice WHERE invoice_id=?", values);
  }

  public int markPaid(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE invoice SET status='PAID',payment_method=?,transaction_id=?,paid_at=NOW() WHERE"
            + " invoice_id=? AND status='UNPAID'",
        values);
  }

  public int updateTotal(Connection c, Object... values) throws SQLException {
    return Jdbc.update(c, "UPDATE invoice SET total_amount=? WHERE invoice_id=?", values);
  }

  public Map<String, Object> calculateTotalRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT COUNT(*) n,COALESCE(SUM(quantity*unit_price),0) total FROM invoice_line WHERE"
            + " invoice_id=?",
        values);
  }

  public int addMedicationLines(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price)"
            + " SELECT ?,'MEDICATION',pd.id,m.name,pd.quantity,pd.unit_price FROM"
            + " prescription_detail pd JOIN prescription p USING(prescription_id) JOIN medication m"
            + " USING(medication_id) WHERE p.record_id=? AND NOT EXISTS(SELECT 1 FROM invoice_line"
            + " l WHERE l.source_type='MEDICATION' AND l.source_id=pd.id)",
        values);
  }

  public int addClinicalLines(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price)"
            + " SELECT ?,'ORDER',o.order_id,s.name,1,o.unit_price FROM clinical_order o JOIN"
            + " service s USING(service_id) WHERE o.record_id=? AND o.status<>'CANCELLED' AND NOT"
            + " EXISTS(SELECT 1 FROM invoice_line l WHERE l.source_type='ORDER' AND"
            + " l.source_id=o.order_id)",
        values);
  }

  public int addConsultationLine(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price)"
            + " SELECT ?,'CONSULTATION',a.appointment_id,'Phi kham',1,a.consultation_fee FROM"
            + " appointment a WHERE a.appointment_id=? AND NOT EXISTS(SELECT 1 FROM invoice_line l"
            + " WHERE l.source_type='CONSULTATION' AND l.source_id=a.appointment_id)",
        values);
  }

  public long insertInvoiceRow(Connection c, Object... values) throws SQLException {
    return Jdbc.insert(
        c,
        "INSERT INTO invoice(invoice_code,record_id,patient_id,cashier_id,total_amount)"
            + " VALUES(?,?,?,?,0)",
        values);
  }

  public Map<String, Object> findPendingRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(
        c,
        "SELECT invoice_id FROM invoice WHERE record_id=? AND status='UNPAID' FOR UPDATE",
        values);
  }

  public java.util.List<java.util.Map<String, Object>> findInvoiceLines(Object... values) {
    return Jdbc.read(
        "SELECT *,quantity*unit_price amount FROM invoice_line WHERE invoice_id=?", values);
  }

  public java.util.List<java.util.Map<String, Object>> findInvoiceDetails(Object... values) {
    return Jdbc.read(
        "SELECT i.*,u.full_name patient_name FROM invoice i JOIN users u ON u.user_id=i.patient_id"
            + " WHERE invoice_id=?",
        values);
  }

  public java.util.List<java.util.Map<String, Object>> findBillableRecords(Object... values) {
    return Jdbc.read(
        "SELECT m.record_id,u.full_name patient_name,m.diagnosis FROM medical_record m JOIN users u"
            + " ON u.user_id=m.patient_id ORDER BY record_id DESC",
        values);
  }

  public vn.hospital.model.Invoice findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM invoice WHERE invoice_id=?", id),
                vn.hospital.model.Invoice.class));
  }

  public java.util.List<java.util.Map<String, Object>> findByPatient(Connection c, long patient)
      throws SQLException {
    return Jdbc.list(
        c,
        "SELECT invoice_id,invoice_code,record_id,total_amount,status,payment_method,paid_at FROM"
            + " invoice WHERE patient_id=? ORDER BY invoice_id DESC",
        patient);
  }

  public boolean hasUnbilledCosts(Connection c, long record) throws SQLException {
    return Jdbc.one(
            c,
            "SELECT m.record_id FROM medical_record m WHERE m.record_id=? AND (NOT EXISTS(SELECT 1"
                + " FROM invoice_line l WHERE l.source_type='CONSULTATION' AND"
                + " l.source_id=m.appointment_id) OR EXISTS(SELECT 1 FROM clinical_order o WHERE"
                + " o.record_id=m.record_id AND o.status<>'CANCELLED' AND NOT EXISTS(SELECT 1 FROM"
                + " invoice_line l WHERE l.source_type='ORDER' AND l.source_id=o.order_id)) OR"
                + " EXISTS(SELECT 1 FROM prescription p JOIN prescription_detail pd"
                + " USING(prescription_id) WHERE p.record_id=m.record_id AND NOT EXISTS(SELECT 1"
                + " FROM invoice_line l WHERE l.source_type='MEDICATION' AND l.source_id=pd.id)))",
            record)
        != null;
  }

  public vn.hospital.model.Invoice lockForClinicalOrder(Connection c, int recordId, int orderId)
      throws SQLException {
    // Khóa hóa đơn trước dòng phí; đọc hiện tại để thấy hóa đơn vừa được lập ở giao dịch khác.
    Jdbc.list(
        c,
        "SELECT invoice_id FROM invoice WHERE record_id=? ORDER BY invoice_id FOR UPDATE",
        recordId);
    var row =
        Jdbc.one(
            c,
            "SELECT invoice_id FROM invoice_line WHERE source_type='ORDER' AND source_id=? FOR"
                + " UPDATE",
            orderId);
    if (row == null) return null;
    long invoiceId = ((Number) row.get("invoice_id")).longValue();
    return BeanMapper.map(findById(c, invoiceId), vn.hospital.model.Invoice.class);
  }

  public void removeClinicalLine(Connection c, int invoiceId, int orderId) throws SQLException {
    Jdbc.update(
        c,
        "DELETE FROM invoice_line WHERE invoice_id=? AND source_type='ORDER' AND source_id=?",
        invoiceId,
        orderId);
  }

  public void recalculateAfterCancellation(Connection c, int invoiceId) throws SQLException {
    Jdbc.update(
        c,
        "UPDATE invoice SET total_amount=(SELECT COALESCE(SUM(quantity*unit_price),0) FROM"
            + " invoice_line WHERE invoice_id=?) WHERE invoice_id=? AND status='UNPAID'",
        invoiceId,
        invoiceId);
    Jdbc.update(
        c,
        "UPDATE invoice SET status='CANCELLED' WHERE invoice_id=? AND status='UNPAID' AND NOT"
            + " EXISTS(SELECT 1 FROM invoice_line WHERE invoice_id=?)",
        invoiceId,
        invoiceId);
  }
}
