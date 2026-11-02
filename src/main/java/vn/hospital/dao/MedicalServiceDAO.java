package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.model.MedicalService;
import vn.hospital.util.Database;

public class MedicalServiceDAO {
  public Map<String, Object> findConsultationFeeRow(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT unit_price FROM service WHERE category='CONSULTATION' AND status='ACTIVE' ORDER BY"
            + " service_id LIMIT 1",
        values);
  }

  public int updateCatalogRow(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE service SET department_id=?,name=?,description=?,category=?,unit_price=?,status=?"
            + " WHERE service_id=?",
        values);
  }

  public long insertCatalogRow(Connection c, Object... values) throws SQLException {
    return Jdbc.insert(
        c,
        "INSERT INTO service(department_id,name,description,category,unit_price,status)"
            + " VALUES(?,?,?,?,?,?)",
        values);
  }

  public List<Map<String, Object>> findDepartments(Object... values) {
    return Jdbc.read("SELECT * FROM departments", values);
  }

  public List<Map<String, Object>> findCatalogServices(Object... values) {
    return Jdbc.read(
        "SELECT s.*,d.name department FROM service s JOIN departments d USING(department_id) ORDER"
            + " BY service_id",
        values);
  }

  public MedicalService findById(Integer id) {
    return Database.tx(c -> findById(c, id));
  }

  public MedicalService findById(Connection c, long id) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(c, "SELECT * FROM service WHERE service_id=?", id), MedicalService.class);
  }

  public List<MedicalService> findAllActive() {
    return Database.tx(c -> findAllActive(c));
  }

  public List<MedicalService> findAllActive(Connection c) throws SQLException {
    return BeanMapper.list(
        Jdbc.list(c, "SELECT * FROM service WHERE status='ACTIVE' ORDER BY category,name"),
        MedicalService.class);
  }

  /** Đọc các dịch vụ thành phần; trả cả dịch vụ ngừng hoạt động để Service kiểm tra. */
  public List<MedicalService> findPackageComponents(Connection connection, int packageId)
      throws SQLException {
    return BeanMapper.list(
        Jdbc.list(
            connection,
            "SELECT s.* FROM service_package_item p JOIN service s ON"
                + " s.service_id=p.component_service_id WHERE p.package_service_id=? ORDER BY"
                + " s.service_id",
            packageId),
        MedicalService.class);
  }

  /** Thay danh sách thành phần trong cùng transaction với thay đổi gói. */
  public void savePackageComponents(
      Connection connection, long packageId, List<Integer> componentIds) throws SQLException {
    Jdbc.update(
        connection, "DELETE FROM service_package_item WHERE package_service_id=?", packageId);
    for (int componentId : componentIds) {
      Jdbc.update(
          connection,
          "INSERT INTO service_package_item(package_service_id,component_service_id) VALUES(?,?)",
          packageId,
          componentId);
    }
  }
}
