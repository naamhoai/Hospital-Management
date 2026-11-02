package vn.hospital.service;

import java.math.BigDecimal;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.model.MedicalService;
import vn.hospital.util.*;

public class ServiceCatalogService {
  /**
   * SQ09/WF03 - Lưu dịch vụ hoặc gói khám. Đầu vào: mã (0 khi thêm), khoa, tên, mô tả, loại, giá,
   * trạng thái và mã thành phần. Kiểm tra quyền quản lý; gói phải có ít nhất hai dịch vụ
   * LAB/IMAGING đang hoạt động. Đầu ra: lưu danh mục/thành phần trong một transaction. Không sửa
   * giá đã chụp ở các lượt khám cũ.
   */
  public void save(
      AuthUser actor,
      long id,
      long department,
      String name,
      String description,
      String category,
      BigDecimal price,
      String status,
      List<Integer> components) {
    Actors.require(actor, "MANAGER", "ADMIN");
    String title = Checks.text(name, 100, "Tên dịch vụ");
    Checks.require(description != null && description.length() <= 500, "Mô tả tối đa 500 ký tự.");
    Checks.require(
        Set.of("CONSULTATION", "LAB", "IMAGING", "PACKAGE").contains(category)
            && Set.of("ACTIVE", "INACTIVE").contains(status),
        "Loại hoặc trạng thái không hợp lệ.");
    Checks.require(
        price != null
            && price.signum() >= 0
            && price.scale() <= 2
            && price.compareTo(new BigDecimal("99999999.99")) <= 0,
        "Đơn giá không hợp lệ.");
    List<Integer> members = category.equals("PACKAGE") ? components : List.of();
    Checks.require(
        members != null && members.size() <= 20 && new HashSet<>(members).size() == members.size(),
        "Danh sách thành phần không hợp lệ.");
    if (category.equals("PACKAGE"))
      Checks.require(members.size() >= 2, "Gói khám cần ít nhất hai dịch vụ.");
    Database.tx(
        connection -> {
          Checks.require(
              new DepartmentDAO().findDepartmentRows(connection, department) != null,
              "Chuyên khoa không tồn tại.");
          var services = new MedicalServiceDAO();
          for (int memberId : members) {
            MedicalService member = services.findById(connection, memberId);
            Checks.require(
                memberId != id
                    && member != null
                    && "ACTIVE".equals(member.getStatus())
                    && Set.of("LAB", "IMAGING").contains(member.getCategory()),
                "Thành phần phải là xét nghiệm/chẩn đoán hình ảnh đang hoạt động, không lồng gói"
                    + " khác.");
          }
          long savedId;
          if (id == 0) {
            savedId =
                services.insertCatalogRow(
                    connection, department, title, description, category, price, status);
          } else {
            Checks.require(
                services.updateCatalogRow(
                        connection, department, title, description, category, price, status, id)
                    == 1,
                "Dịch vụ không tồn tại.");
            savedId = id;
          }
          services.savePackageComponents(connection, savedId, members);
          AuditDAO.record(connection, actor.getUserId(), "SAVE_SERVICE", "service", savedId);
          return null;
        });
  }

  public List<Map<String, Object>> findDepartments() {
    return new MedicalServiceDAO().findDepartments();
  }

  public List<Map<String, Object>> findCatalogServices() {
    return new MedicalServiceDAO().findCatalogServices();
  }

  public List<MedicalService> componentOptions() {
    return new MedicalServiceDAO()
        .findAllActive().stream()
            .filter(service -> Set.of("LAB", "IMAGING").contains(service.getCategory()))
            .toList();
  }
}
