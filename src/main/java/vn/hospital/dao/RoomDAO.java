package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class RoomDAO {
  public Map<String, Object> findMatchingRoomRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT room_id FROM rooms WHERE room_number=? AND department_id=? AND"
            + " room_type='CONSULTATION' AND status='ACTIVE'",
        values);
  }

  public vn.hospital.model.Room findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM rooms WHERE room_id=?", id),
                vn.hospital.model.Room.class));
  }
}
