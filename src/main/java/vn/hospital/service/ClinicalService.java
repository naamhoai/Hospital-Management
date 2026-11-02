package vn.hospital.service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class ClinicalService {
  private final AuthUser actor;

  public ClinicalService(AuthUser actor) {
    this.actor = actor;
  }

  public List<MedicalService> getServices() {
    Actors.require(actor, "DOCTOR", "LAB_TECHNICIAN");
    return new MedicalServiceDAO()
        .findAllActive().stream().filter(s -> !"CONSULTATION".equals(s.getCategory())).toList();
  }

  /**
   * SQ18 - Tạo chỉ định cận lâm sàng. Đầu vào: ClinicalOrderRequest gồm recordId, serviceIds và chỉ
   * dẫn; bác sĩ lấy từ phiên. Yêu cầu bệnh án OPEN, đúng bác sĩ và đã có chẩn đoán sơ bộ. Dịch vụ
   * lẻ tạo một chỉ định; gói được tách thành các chỉ định thành phần, phân bổ đúng giá gói. Đầu ra:
   * List<ClinicalOrder> đã lưu, PENDING/PENDING_PAYMENT; chưa tự làm xét nghiệm hay thu tiền.
   */
  public List<ClinicalOrder> createClinicalOrders(ClinicalOrderRequest request) {
    Actors.require(actor, "DOCTOR");
    Checks.require(
        request != null
            && request.getRecordId() != null
            && request.getServiceIds() != null
            && !request.getServiceIds().isEmpty()
            && request.getServiceIds().size() <= 20,
        "Chọn từ 1 đến 20 dịch vụ.");
    Checks.require(
        new HashSet<>(request.getServiceIds()).size() == request.getServiceIds().size(),
        "Dịch vụ bị lặp.");
    Checks.require(
        request.getInstructions() == null || request.getInstructions().length() <= 255,
        "Chỉ dẫn tối đa 255 ký tự.");
    return Database.tx(
        connection -> {
          MedicalRecord record =
              EMRService.editable(
                  new MedicalRecordDAO().findById(connection, request.getRecordId()), actor, true);
          Checks.require(
              record.getDiagnosis() != null && !record.getDiagnosis().isBlank(),
              "Cần lưu chẩn đoán sơ bộ trước khi chỉ định.");
          var dao = new ClinicalOrderDAO();
          var orders = new ArrayList<ClinicalOrder>();
          var selectedServices = new MedicalServiceDAO();
          var expandedIds = new HashSet<Integer>();
          for (Integer id : request.getServiceIds()) {
            Checks.require(id != null && id > 0, "Dịch vụ không hợp lệ.");
            MedicalService service = selectedServices.findById(connection, id);
            requireOrderable(service);
            if (!"PACKAGE".equals(service.getCategory())) {
              addOrder(
                  connection,
                  orders,
                  expandedIds,
                  record.getRecordId(),
                  service,
                  null,
                  service.getUnitPrice(),
                  request.getInstructions());
              continue;
            }
            // Một gói được tách thành các chỉ định để Lab nhập từng kết quả riêng.
            var members = selectedServices.findPackageComponents(connection, id);
            Checks.require(members.size() >= 2, "Gói chưa có đủ thành phần.");
            BigDecimal total = BigDecimal.ZERO;
            for (var member : members) {
              requireOrderable(member);
              Checks.require(
                  !"PACKAGE".equals(member.getCategory()), "Không hỗ trợ gói lồng nhau.");
              total = total.add(member.getUnitPrice());
            }
            Checks.require(
                total.signum() > 0 || service.getUnitPrice().signum() == 0,
                "Gói có giá nhưng tổng giá thành phần bằng 0.");
            BigDecimal remaining = service.getUnitPrice();
            for (int index = 0; index < members.size(); index++) {
              var member = members.get(index);
              // Làm tròn xuống các dòng đầu; dòng cuối nhận phần dư để tổng đúng giá gói.
              BigDecimal price =
                  index == members.size() - 1
                      ? remaining
                      : total.signum() == 0
                          ? BigDecimal.ZERO
                          : service
                              .getUnitPrice()
                              .multiply(member.getUnitPrice())
                              .divide(total, 2, RoundingMode.DOWN);
              remaining = remaining.subtract(price);
              addOrder(
                  connection,
                  orders,
                  expandedIds,
                  record.getRecordId(),
                  member,
                  id,
                  price,
                  request.getInstructions());
            }
          }
          dao.saveAll(connection, orders);
          for (var order : orders)
            AuditDAO.record(
                connection, actor.getUserId(), "ORDER_TEST", "clinical_order", order.getOrderId());
          List<ClinicalOrder> savedOrders = new ArrayList<>();
          for (ClinicalOrder order : orders) {
            savedOrders.add(dao.findById(connection, order.getOrderId()));
          }
          return savedOrders;
        });
  }

  private void requireOrderable(MedicalService service) {
    Checks.require(
        service != null
            && "ACTIVE".equals(service.getStatus())
            && !"CONSULTATION".equals(service.getCategory()),
        "Dịch vụ không còn hoạt động.");
  }

  /** Chụp giá vào clinical_order; một dịch vụ không được lặp giữa gói và lựa chọn lẻ. */
  private void addOrder(
      java.sql.Connection connection,
      List<ClinicalOrder> orders,
      Set<Integer> ids,
      int recordId,
      MedicalService service,
      Integer packageId,
      BigDecimal price,
      String instructions)
      throws java.sql.SQLException {
    Checks.require(ids.add(service.getServiceId()), "Dịch vụ bị trùng giữa các lựa chọn/gói khám.");
    Checks.require(
        new ClinicalOrderDAO().findExisting(connection, recordId, service.getServiceId()) == null,
        "Dịch vụ đã được chỉ định cho bệnh án này.");
    var order = new ClinicalOrder();
    order.setRecordId(recordId);
    order.setServiceId(service.getServiceId());
    order.setPackageServiceId(packageId);
    order.setInstructions(instructions);
    order.setUnitPrice(price);
    orders.add(order);
  }

  /** Hủy chỉ định chưa trả tiền; hóa đơn và chỉ định cùng thành công hoặc cùng rollback. */
  public Boolean cancelOrder(Integer orderId) {
    Actors.require(actor, "DOCTOR");
    return Database.tx(
        connection -> {
          var orders = new ClinicalOrderDAO();
          Integer recordId = orders.findRecordId(connection, orderId);
          Checks.require(recordId != null, "Không tìm thấy chỉ định.");
          EMRService.editable(new MedicalRecordDAO().findById(connection, recordId), actor, true);
          // Giữ cùng thứ tự khóa với thu ngân: bệnh án -> hóa đơn -> chỉ định.
          Invoice invoice = new InvoiceDAO().lockForClinicalOrder(connection, recordId, orderId);
          ClinicalOrder order = orders.findById(connection, orderId);
          Checks.require(
              order != null && Set.of("PENDING", "RESULT_RECALLED").contains(order.getStatus()),
              "Chỉ hủy chỉ định chưa thực hiện.");
          Checks.require(
              !"PAID".equals(order.getPaymentStatus()),
              "Chỉ định đã thanh toán, không thể hủy trực tiếp.");
          new BillingService()
              .removeUnpaidClinicalCharge(connection, invoice, orderId, actor.getUserId());
          orders.updateExecutionStatus(connection, orderId, "CANCELLED");
          AuditDAO.record(connection, actor.getUserId(), "CANCEL_TEST", "clinical_order", orderId);
          return true;
        });
  }

  /**
   * SQ19 - Nhập và duyệt kết quả. Đầu vào: orderId, nội dung/kết luận kết quả; kỹ thuật viên lấy từ
   * phiên. Gọi cùng hàm lưu có upload=null. Chỉ được duyệt chỉ định đã trả phí, chưa có kết quả
   * hiệu lực. Đầu ra: TestResult mới và clinical_order COMPLETED trong cùng transaction.
   */
  public TestResult recordTestResult(Integer orderId, TestResult result) {
    return recordTestResult(orderId, result, null);
  }

  /** Chỉ duyệt chỉ định đã thanh toán; khóa dòng chỉ định để tránh hai lần duyệt đồng thời. */
  public TestResult recordTestResult(Integer orderId, TestResult result, UploadData upload) {
    Actors.require(actor, "LAB_TECHNICIAN");
    Checks.require(result != null, "Thiếu dữ liệu kết quả.");
    result.setResultData(Checks.text(result.getResultData(), 10000, "Dữ liệu kết quả"));
    Checks.require(
        result.getConclusion() == null || result.getConclusion().length() <= 255,
        "Kết luận tối đa 255 ký tự.");
    String url = result.getFileUrl() == null ? "" : result.getFileUrl().trim();
    Checks.require(
        url.isEmpty() || (url.startsWith("https://") && url.length() <= 255),
        "URL kết quả phải dùng HTTPS và tối đa 255 ký tự.");
    Checks.require(
        upload == null || url.isEmpty(), "Chỉ chọn tải tệp hoặc nhập URL, không dùng cả hai.");
    AttachmentStorage storage = new AttachmentStorage();
    String storedFile = upload == null ? null : storage.save(upload);
    try {
      return Database.tx(
          connection -> {
            var orders = new ClinicalOrderDAO();
            ClinicalOrder order = orders.findById(connection, orderId);
            Checks.require(
                order != null
                    && "PAID".equals(order.getPaymentStatus())
                    && Set.of("PENDING", "RESULT_RECALLED").contains(order.getStatus()),
                "Chỉ duyệt kết quả cho chỉ định đã thanh toán, chưa hoàn tất.");
            Checks.require(
                new TestResultDAO().findByOrderId(connection, orderId) == null,
                "Kết quả đã duyệt, không được sửa.");

            result.setFileUrl(upload == null ? url : storedFile);
            result.setOrderId(orderId);
            result.setTechnicianId(Actors.id(actor.getUserId()));
            TestResult saved = new TestResultDAO().save(connection, result);
            orders.updateExecutionStatus(connection, orderId, "COMPLETED");
            var record = new MedicalRecordDAO().findByIdRead(connection, order.getRecordId());
            AppointmentService.notify(
                connection,
                record.getDoctorId(),
                "Đã có kết quả cận lâm sàng",
                "Chỉ định #" + orderId + " đã được duyệt. Mở lại bệnh án để xem kết quả.");
            AppointmentService.notify(
                connection,
                record.getPatientId(),
                "Đã có kết quả xét nghiệm",
                "Kết quả chỉ định #" + orderId + " đã được duyệt. Đăng nhập để xem kết quả.");
            AuditDAO.record(
                connection,
                actor.getUserId(),
                "APPROVE_RESULT",
                "test_result",
                saved.getResultId());
            return saved;
          });
    } catch (RuntimeException e) {
      storage.deleteOwn(storedFile);
      throw e;
    }
  }

  public Map<String, Object> labWorkspace(String keyword, String status, Integer selected) {
    Actors.require(actor, "LAB_TECHNICIAN");
    Checks.require(
        Set.of("", "PENDING", "COMPLETED", "CANCELLED", "RESULT_RECALLED").contains(status)
            && keyword.length() <= 100,
        "Bộ lọc không hợp lệ.");
    return Database.read(
        connection -> {
          Map<String, Object> page = new LinkedHashMap<>();
          page.put("orders", new ClinicalOrderDAO().worklist(connection, keyword, status));
          if (selected != null) {
            ClinicalOrder order = new ClinicalOrderDAO().findByIdRead(connection, selected);
            Checks.require(order != null, "Không tìm thấy chỉ định.");
            MedicalRecord record =
                new MedicalRecordDAO().findByIdRead(connection, order.getRecordId());
            page.put("selectedOrder", order);
            page.put("recallHistory", new ResultRecallDAO().findByOrder(connection, selected));
            page.put(
                "patient",
                new MedicalRecordDAO().patientProfile(connection, record.getPatientId()));
          }
          return page;
        });
  }

  public File getAttachment(Integer resultId) {
    Actors.require(actor, "DOCTOR", "LAB_TECHNICIAN", "PATIENT");
    return Database.read(
        connection -> {
          TestResult result = new TestResultDAO().findById(connection, resultId);
          Checks.require(
              result != null && "APPROVED".equals(result.getStatus()),
              "Kết quả không còn hiệu lực để tải xuống.");
          ClinicalOrder order =
              new ClinicalOrderDAO().findByIdRead(connection, result.getOrderId());
          MedicalRecord record =
              new MedicalRecordDAO().findByIdRead(connection, order.getRecordId());
          Checks.require(
              actor.is("LAB_TECHNICIAN")
                  || (actor.is("DOCTOR") && record.getDoctorId().longValue() == actor.getUserId())
                  || (actor.is("PATIENT")
                      && record.getPatientId().longValue() == actor.getUserId()),
              "Bạn không có quyền tải tệp này.");
          return new AttachmentStorage().resolve(result.getFileUrl());
        });
  }

  public File printClinicalOrder(Integer id) {
    Actors.require(actor, "DOCTOR");
    var rows =
        Database.read(
            connection -> {
              var order = new ClinicalOrderDAO().findByIdRead(connection, id);
              Checks.require(order != null, "Không tìm thấy chỉ định.");
              var record =
                  EMRService.editable(
                      new MedicalRecordDAO().findByIdRead(connection, order.getRecordId()),
                      actor,
                      false);
              var patient =
                  new MedicalRecordDAO().patientProfile(connection, record.getPatientId());
              Map<String, Object> fields = new LinkedHashMap<>();
              fields.put("Bệnh nhân", patient.get("full_name"));
              fields.put(
                  "Bác sĩ", new MedicalRecordDAO().doctorName(connection, record.getDoctorId()));
              fields.put("Bệnh án", record.getRecordId());
              fields.put("Chẩn đoán sơ bộ", record.getDiagnosis());
              fields.put("Dịch vụ", order.getService().getName());
              fields.put("Chỉ dẫn", order.getInstructions());
              fields.put("Ngày chỉ định", order.getOrderDate());
              fields.put("Đơn giá (VND)", order.getUnitPrice());
              fields.put("Thanh toán", order.getPaymentStatus());
              fields.put("Trạng thái", order.getStatus());
              return List.of(fields);
            });
    return new ReportExportService().createPDF("PHIẾU CHỈ ĐỊNH #" + id, rows);
  }
}
