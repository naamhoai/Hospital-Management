package vn.hospital.dao;

import java.util.List;
import vn.hospital.model.Role;
import vn.hospital.util.Database;

public class RoleDAO {
  public List<Role> findAll() {
    return Database.read(
        c -> BeanMapper.list(Jdbc.list(c, "SELECT * FROM roles ORDER BY role_id"), Role.class));
  }
}
