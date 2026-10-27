package vn.hospital.service;

import java.math.BigDecimal;
import java.util.Set;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class CatalogService {
  public void save(
      AuthUser actor,
      long id,
      long department,
      String name,
      String category,
      BigDecimal price,
      String status) {
    String title = Checks.text(name, 100, "Tên dịch vụ");
    Checks.require(
        Set.of("CONSULTATION", "LAB", "IMAGING").contains(category)
            && Set.of("ACTIVE", "INACTIVE").contains(status),
        "Loại hoặc trạng thái dịch vụ không hợp lệ.");
    Database.tx(
        c -> {
          Checks.require(
              new DepartmentDAO().findDepartmentRows(c, department) != null,
              "Chuyên khoa không tồn tại.");
          long key = id;
          if (id == 0)
            key =
                new MedicalServiceDAO()
                    .insertCatalogRow(c, department, title, category, price, status);
          else
            Checks.require(
                new MedicalServiceDAO()
                        .updateCatalogRow(c, department, title, category, price, status, id)
                    == 1,
                "Dịch vụ không tồn tại.");
          AuditDAO.record(c, actor.getUserId(), "SAVE_SERVICE", "service", key);
          return null;
        });
  }

  public java.util.List<java.util.Map<String, Object>> findDepartments(Object... values) {
    return new vn.hospital.dao.MedicalServiceDAO().findDepartments(values);
  }

  public java.util.List<java.util.Map<String, Object>> findCatalogServices(Object... values) {
    return new vn.hospital.dao.MedicalServiceDAO().findCatalogServices(values);
  }
}
