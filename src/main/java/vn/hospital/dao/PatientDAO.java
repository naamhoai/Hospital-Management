package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class PatientDAO {
  public int blockBooking(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE patients SET booking_blocked_until=DATE_ADD(CURDATE(),INTERVAL 30 DAY) WHERE"
            + " patient_id=?",
        values);
  }

  public Map<String, Object> lockPatient(Connection c, Object... values) throws SQLException {
    return Jdbc.one(c, "SELECT patient_id FROM patients WHERE patient_id=? FOR UPDATE", values);
  }

  public Map<String, Object> lockForBooking(Connection c, Object... values) throws SQLException {
    return Jdbc.one(c, "SELECT * FROM patients WHERE patient_id=? FOR UPDATE", values);
  }

  public vn.hospital.model.Patient findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM patients WHERE patient_id=?", id),
                vn.hospital.model.Patient.class));
  }
}
