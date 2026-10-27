package vn.hospital.dao;

import java.sql.*;
import java.util.*;

public class NotificationDAO {
  public int enqueue(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "INSERT INTO notification_outbox(recipient,subject,body) SELECT email,?,? FROM users WHERE"
            + " user_id=?",
        values);
  }

  public List<Map<String, Object>> pending() {
    return Jdbc.read(
        "SELECT * FROM notification_outbox WHERE status='PENDING' AND attempts<5 ORDER BY id LIMIT"
            + " 10");
  }

  public void recordDelivery(Connection c, Object id, boolean sent) throws SQLException {
    Jdbc.update(
        c,
        "UPDATE notification_outbox SET status=?,attempts=attempts+1,last_error=? WHERE id=?",
        sent ? "SENT" : "PENDING",
        sent ? null : "SMTP delivery failed; check configuration",
        id);
  }
}
