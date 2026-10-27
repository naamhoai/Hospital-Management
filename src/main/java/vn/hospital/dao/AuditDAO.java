package vn.hospital.dao;

import java.sql.*;

public final class AuditDAO {
  private AuditDAO() {}

  public static void record(Connection c, Long actor, String action, String entity, long id)
      throws SQLException {
    Jdbc.update(
        c,
        "INSERT INTO audit_log(actor_id,action,entity_name,entity_id) VALUES(?,?,?,?)",
        actor,
        action,
        entity,
        id);
  }
}
