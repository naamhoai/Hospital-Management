package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class DoctorDAO {
  public Map<String, Object> lockDoctor(Connection c, Object... values) throws SQLException {
    return Jdbc.one(c, "SELECT doctor_id FROM doctors WHERE doctor_id=? FOR UPDATE", values);
  }

  public Map<String, Object> findActiveRoomRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT d.room_number FROM doctors d JOIN rooms r ON d.room_number=r.room_number WHERE"
            + " doctor_id=? AND r.status='ACTIVE'",
        values);
  }

  public int recalculateRating(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE doctors SET rating_average=(SELECT AVG(rating) FROM feedback WHERE doctor_id=?)"
            + " WHERE doctor_id=?",
        values);
  }

  public vn.hospital.model.Doctor findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM doctors WHERE doctor_id=?", id),
                vn.hospital.model.Doctor.class));
  }
}
