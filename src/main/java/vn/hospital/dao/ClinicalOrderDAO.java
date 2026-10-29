package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class ClinicalOrderDAO {
  public ClinicalOrder findById(Integer id) {
    return Database.tx(c -> findById(c, id));
  }

  public ClinicalOrder findById(Connection c, long id) throws SQLException {
    return enrich(
        c,
        BeanMapper.map(
            Jdbc.one(c, "SELECT * FROM clinical_order WHERE order_id=? FOR UPDATE", id),
            ClinicalOrder.class));
  }

  public List<ClinicalOrder> findByRecordId(Integer id) {
    return Database.tx(c -> findByRecordId(c, id));
  }

  public List<ClinicalOrder> findByRecordId(Connection c, long id) throws SQLException {
    List<ClinicalOrder> items =
        BeanMapper.list(
            Jdbc.list(
                c, "SELECT * FROM clinical_order WHERE record_id=? ORDER BY order_id DESC", id),
            ClinicalOrder.class);
    for (var item : items) enrich(c, item);
    return items;
  }

  private ClinicalOrder enrich(Connection c, ClinicalOrder order) throws SQLException {
    if (order != null) {
      order.setService(new MedicalServiceDAO().findById(c, order.getServiceId()));
      order.setResult(new TestResultDAO().findByOrderId(c, order.getOrderId()));
    }
    return order;
  }

  public List<ClinicalOrder> saveAll(List<ClinicalOrder> orders) {
    return Database.tx(c -> saveAll(c, orders));
  }

  public List<ClinicalOrder> saveAll(Connection c, List<ClinicalOrder> orders) throws SQLException {
    for (var order : orders) {
      order.setOrderId(
          Actors.id(
              Jdbc.insert(
                  c,
                  "INSERT INTO clinical_order(record_id,service_id,instructions,unit_price)"
                      + " VALUES(?,?,?,?)",
                  order.getRecordId(),
                  order.getServiceId(),
                  order.getInstructions(),
                  order.getUnitPrice())));
    }
    return orders;
  }

  public Boolean updateExecutionStatus(Integer id, String status) {
    return Database.tx(c -> updateExecutionStatus(c, id, status));
  }

  public boolean updateExecutionStatus(Connection c, long id, String status) throws SQLException {
    return Jdbc.update(c, "UPDATE clinical_order SET status=? WHERE order_id=?", status, id) == 1;
  }

  public ClinicalOrder findExisting(Connection c, long record, long service) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(
            c,
            "SELECT * FROM clinical_order WHERE record_id=? AND service_id=? FOR UPDATE",
            record,
            service),
        ClinicalOrder.class);
  }

  public boolean isBilled(Connection c, long order) throws SQLException {
    return Jdbc.one(
            c, "SELECT line_id FROM invoice_line WHERE source_type='ORDER' AND source_id=?", order)
        != null;
  }

  public boolean hasUnfinished(Connection c, long record) throws SQLException {
    return Jdbc.one(
            c,
            "SELECT order_id FROM clinical_order WHERE record_id=? AND status NOT IN"
                + " ('COMPLETED','CANCELLED') LIMIT 1",
            record)
        != null;
  }

  public int markPaidByInvoice(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE clinical_order SET payment_status='PAID' WHERE order_id IN (SELECT source_id FROM"
            + " invoice_line WHERE invoice_id=? AND source_type='ORDER')",
        values);
  }

  public List<Map<String, Object>> worklist(Connection c, String keyword, String status)
      throws SQLException {
    // Bản đã thu hồi chỉ nằm trong lịch sử; JOIN bản hiệu lực để không nhân đôi chỉ định.
    return Jdbc.list(
        c,
        "SELECT o.*,s.name service_name,u.full_name"
            + " patient_name,m.patient_id,m.doctor_id,t.result_id,t.result_data,t.conclusion,t.file_url"
            + " FROM clinical_order o JOIN service s USING(service_id) JOIN medical_record m"
            + " USING(record_id) JOIN users u ON u.user_id=m.patient_id LEFT JOIN test_result t ON"
            + " t.order_id=o.order_id AND t.status='APPROVED' WHERE (u.full_name LIKE ? OR"
            + " CAST(o.order_id AS CHAR)=?) AND (?='' OR o.status=?) ORDER BY CASE WHEN"
            + " o.payment_status='PAID' AND o.status IN ('PENDING','RESULT_RECALLED') THEN 0 ELSE 1"
            + " END,o.order_id DESC LIMIT 100",
        "%" + keyword + "%",
        keyword,
        status,
        status);
  }

  public Integer findRecordId(Connection c, long order) throws SQLException {
    var row = Jdbc.one(c, "SELECT record_id FROM clinical_order WHERE order_id=?", order);
    return row == null ? null : ((Number) row.get("record_id")).intValue();
  }

  public ClinicalOrder findByIdRead(Connection c, long id) throws SQLException {
    return enrich(
        c,
        BeanMapper.map(
            Jdbc.one(c, "SELECT * FROM clinical_order WHERE order_id=?", id), ClinicalOrder.class));
  }
}
