package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class DepartmentDAO {
  public Map<String, Object> findDepartmentRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(c, "SELECT department_id FROM departments WHERE department_id=?", values);
  }

  public vn.hospital.model.Department findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM departments WHERE department_id=?", id),
                vn.hospital.model.Department.class));
  }
}
