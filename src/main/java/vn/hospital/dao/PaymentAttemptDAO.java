package vn.hospital.dao;

import java.sql.*;
import java.util.*;

public class PaymentAttemptDAO {
  public Map<String, Object> findPendingRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(
        c,
        "SELECT attempt_id FROM payment_attempt WHERE invoice_id=? AND status='PENDING' AND"
            + " created_at>DATE_SUB(NOW(),INTERVAL 15 MINUTE)",
        values);
  }

  public int markSucceeded(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE payment_attempt SET status='SUCCESS',gateway_transaction_id=? WHERE attempt_id=?",
        values);
  }

  public int markFailed(Connection c, Object... values) throws SQLException {
    return Jdbc.update(c, "UPDATE payment_attempt SET status='FAILED' WHERE attempt_id=?", values);
  }

  public Map<String, Object> lockAttemptRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(c, "SELECT * FROM payment_attempt WHERE attempt_id=? FOR UPDATE", values);
  }

  public Map<String, Object> findInvoiceRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(c, "SELECT invoice_id FROM payment_attempt WHERE attempt_id=?", values);
  }

  public int insertAttempt(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c, "INSERT INTO payment_attempt(attempt_id,invoice_id,amount) VALUES(?,?,?)", values);
  }

  public Map<String, Object> findLatestPendingRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT attempt_id FROM payment_attempt WHERE invoice_id=? AND status='PENDING' AND"
            + " created_at>DATE_SUB(NOW(),INTERVAL 15 MINUTE) ORDER BY created_at DESC LIMIT 1",
        values);
  }

  public void expirePendingAttempts(Connection c, int invoiceId) throws SQLException {
    Jdbc.update(
        c,
        "UPDATE payment_attempt SET status='EXPIRED' WHERE invoice_id=? AND status='PENDING' AND"
            + " created_at<=DATE_SUB(NOW(),INTERVAL 15 MINUTE)",
        invoiceId);
  }
}
