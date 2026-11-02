package vn.hospital.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.hospital.dao.*;
import vn.hospital.dao.MedicalServiceDAO;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class ServiceCatalogService {
  /** Quản lý danh mục; giá mới áp dụng cho phát sinh mới, không sửa giá trên hóa đơn cũ. */
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
        connection -> {
          Checks.require(
              new DepartmentDAO().findDepartmentRows(connection, department) != null,
              "Chuyên khoa không tồn tại.");
          long key = id;
          if (id == 0)
            key =
                new MedicalServiceDAO()
                    .insertCatalogRow(connection, department, title, category, price, status);
          else
            Checks.require(
                new MedicalServiceDAO()
                        .updateCatalogRow(
                            connection, department, title, category, price, status, id)
                    == 1,
                "Dịch vụ không tồn tại.");
          AuditDAO.record(connection, actor.getUserId(), "SAVE_SERVICE", "service", key);
          return null;
        });
  }

  public List<Map<String, Object>> findDepartments(Object... values) {
    return new MedicalServiceDAO().findDepartments(values);
  }

  public List<Map<String, Object>> findCatalogServices(Object... values) {
    return new MedicalServiceDAO().findCatalogServices(values);
  }
}
