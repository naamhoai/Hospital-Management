package vn.hospital.dao;

import java.sql.*;
import java.util.List;
import vn.hospital.model.ClinicalUpdate;

public class ClinicalUpdateDAO {
  private static final String OWN_UPDATES =
      " FROM audit_log a JOIN test_result t ON t.result_id=a.entity_id JOIN clinical_order o ON"
          + " o.order_id=t.order_id JOIN medical_record m ON m.record_id=o.record_id JOIN queue q"
          + " ON q.appointment_id=m.appointment_id JOIN users u ON u.user_id=m.patient_id WHERE"
          + " a.entity_name='test_result' AND a.action IN ('APPROVE_RESULT','RECALL_RESULT') AND"
          + " m.doctor_id=?";

  public long latestId(Connection c, long doctor) throws SQLException {
    return Jdbc.number(
        Jdbc.one(c, "SELECT COALESCE(MAX(a.id),0) latest_id" + OWN_UPDATES, doctor), "latest_id");
  }

  public List<ClinicalUpdate> findAfter(Connection c, long doctor, long after) throws SQLException {
    return BeanMapper.list(
        Jdbc.list(
            c,
            "SELECT a.id,q.queue_id,o.order_id,u.full_name patient_name,a.action"
                + OWN_UPDATES
                + " AND a.id>? ORDER BY a.id LIMIT 20",
            doctor,
            after),
        ClinicalUpdate.class);
  }
}
