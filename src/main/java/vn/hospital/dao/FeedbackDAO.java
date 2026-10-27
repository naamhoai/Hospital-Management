package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class FeedbackDAO {
  public long insertFeedbackRow(Connection c, Object... values) throws SQLException {
    return Jdbc.insert(
        c,
        "INSERT INTO feedback(record_id,patient_id,doctor_id,rating,comment) VALUES(?,?,?,?,?)",
        values);
  }

  public Map<String, Object> findExistingRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(c, "SELECT feedback_id FROM feedback WHERE record_id=?", values);
  }

  public Map<String, Object> lockRecordRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(c, "SELECT * FROM medical_record WHERE record_id=? FOR UPDATE", values);
  }

  public java.util.List<java.util.Map<String, Object>> findEligibleFeedbackRecords(
      Object... values) {
    return Jdbc.read(
        "SELECT m.record_id,m.diagnosis,m.closed_at,u.full_name doctor_name,f.rating FROM"
            + " medical_record m JOIN users u ON u.user_id=m.doctor_id LEFT JOIN feedback f"
            + " USING(record_id) WHERE m.patient_id=? AND m.status='CLOSED' ORDER BY m.record_id"
            + " DESC",
        values);
  }

  public vn.hospital.model.Feedback findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM feedback WHERE feedback_id=?", id),
                vn.hospital.model.Feedback.class));
  }
}
