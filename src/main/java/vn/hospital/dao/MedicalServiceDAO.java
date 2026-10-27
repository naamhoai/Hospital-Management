package vn.hospital.dao;

import java.sql.*;
import java.util.*;
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
        "UPDATE service SET department_id=?,name=?,category=?,unit_price=?,status=? WHERE"
            + " service_id=?",
        values);
  }

  public long insertCatalogRow(Connection c, Object... values) throws SQLException {
    return Jdbc.insert(
        c,
        "INSERT INTO service(department_id,name,category,unit_price,status) VALUES(?,?,?,?,?)",
        values);
  }

  public java.util.List<java.util.Map<String, Object>> findDepartments(Object... values) {
    return Jdbc.read("SELECT * FROM departments", values);
  }

  public java.util.List<java.util.Map<String, Object>> findCatalogServices(Object... values) {
    return Jdbc.read(
        "SELECT s.*,d.name department FROM service s JOIN departments d USING(department_id) ORDER"
            + " BY service_id",
        values);
  }

  public vn.hospital.model.MedicalService findById(Integer id) {
    return Database.tx(c -> findById(c, id));
  }

  public vn.hospital.model.MedicalService findById(Connection c, long id) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(c, "SELECT * FROM service WHERE service_id=?", id),
        vn.hospital.model.MedicalService.class);
  }

  public java.util.List<vn.hospital.model.MedicalService> findAllActive() {
    return Database.tx(c -> findAllActive(c));
  }

  public java.util.List<vn.hospital.model.MedicalService> findAllActive(Connection c)
      throws SQLException {
    return BeanMapper.list(
        Jdbc.list(c, "SELECT * FROM service WHERE status='ACTIVE' ORDER BY category,name"),
        vn.hospital.model.MedicalService.class);
  }
}
