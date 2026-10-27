package vn.hospital.service;

import java.io.File;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class ClinicalService {
  private final AuthUser actor;

  public ClinicalService() {
    this(null);
  }

  public ClinicalService(AuthUser actor) {
    this.actor = actor;
  }

  public List<MedicalService> getServices() {
    Actors.require(actor, "DOCTOR", "LAB_TECHNICIAN");
    return new MedicalServiceDAO()
        .findAllActive().stream().filter(s -> !"CONSULTATION".equals(s.getCategory())).toList();
  }

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
        c -> {
          MedicalRecord record =
              EMRService.editable(
                  new MedicalRecordDAO().findById(c, request.getRecordId()), actor, true);
          Checks.require(
              record.getDiagnosis() != null && !record.getDiagnosis().isBlank(),
              "Cần lưu chẩn đoán sơ bộ trước khi chỉ định.");
          var dao = new ClinicalOrderDAO();
          var orders = new ArrayList<ClinicalOrder>();
          for (Integer id : request.getServiceIds()) {
            Checks.require(id != null && id > 0, "Dịch vụ không hợp lệ.");
            MedicalService service = new MedicalServiceDAO().findById(c, id);
            Checks.require(
                service != null
                    && "ACTIVE".equals(service.getStatus())
                    && !"CONSULTATION".equals(service.getCategory()),
                "Dịch vụ không còn hoạt động.");
            Checks.require(
                dao.findExisting(c, record.getRecordId(), id) == null,
                "Dịch vụ đã được chỉ định cho bệnh án này.");
            var order = new ClinicalOrder();
            order.setRecordId(record.getRecordId());
            order.setServiceId(id);
            order.setInstructions(request.getInstructions());
            order.setUnitPrice(service.getUnitPrice());
            orders.add(order);
          }
          dao.saveAll(c, orders);
          for (var order : orders)
            AuditDAO.record(
                c, actor.getUserId(), "ORDER_TEST", "clinical_order", order.getOrderId());
          return dao.findByRecordId(c, record.getRecordId()).stream()
              .filter(
                  o ->
                      orders.stream()
                          .anyMatch(created -> created.getOrderId().equals(o.getOrderId())))
              .toList();
        });
  }

  public Boolean cancelOrder(Integer id) {
    Actors.require(actor, "DOCTOR");
    return Database.tx(
        c -> {
          var dao = new ClinicalOrderDAO();
          Integer recordId = dao.findRecordId(c, id);
          Checks.require(recordId != null, "Không tìm thấy chỉ định.");
          EMRService.editable(new MedicalRecordDAO().findById(c, recordId), actor, true);
          ClinicalOrder order = dao.findById(c, id);
          Checks.require(order != null, "Không tìm thấy chỉ định.");
          Checks.require(
              "PENDING".equals(order.getStatus()) && !"PAID".equals(order.getPaymentStatus()),
              "Chỉ hủy chỉ định chưa thanh toán, chưa thực hiện.");
          Checks.require(
              !dao.isBilled(c, id),
              "Chỉ định đã có trên hóa đơn. Cần thu ngân xử lý trước khi hủy.");
          boolean result = dao.updateExecutionStatus(c, id, "CANCELLED");
          AuditDAO.record(c, actor.getUserId(), "CANCEL_TEST", "clinical_order", id);
          return result;
        });
  }

  public TestResult recordTestResult(Integer orderId, TestResult result) {
    return recordTestResult(orderId, result, null);
  }

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
    String[] stored = {null};
    try {
      return Database.tx(
          c -> {
            var orders = new ClinicalOrderDAO();
            ClinicalOrder order = orders.findById(c, orderId);
            Checks.require(
                order != null
                    && "PAID".equals(order.getPaymentStatus())
                    && "PENDING".equals(order.getStatus()),
                "Chỉ duyệt kết quả cho chỉ định đã thanh toán, chưa hoàn tất.");
            Checks.require(
                new TestResultDAO().findByOrderId(c, orderId) == null,
                "Kết quả đã duyệt, không được sửa.");
            if (upload != null) stored[0] = new AttachmentStorage().save(upload);
            result.setFileUrl(upload == null ? url : stored[0]);
            result.setOrderId(orderId);
            result.setTechnicianId(Actors.id(actor.getUserId()));
            TestResult saved = new TestResultDAO().save(c, result);
            orders.updateExecutionStatus(c, orderId, "COMPLETED");
            var record = new MedicalRecordDAO().findByIdRead(c, order.getRecordId());
            AppointmentService.notify(
                c,
                record.getDoctorId(),
                "Đã có kết quả cận lâm sàng",
                "Chỉ định #" + orderId + " đã được duyệt. Mở lại bệnh án để xem kết quả.");
            AppointmentService.notify(
                c,
                record.getPatientId(),
                "Đã có kết quả xét nghiệm",
                "Kết quả chỉ định #" + orderId + " đã được duyệt. Đăng nhập để xem kết quả.");
            AuditDAO.record(
                c, actor.getUserId(), "APPROVE_RESULT", "test_result", saved.getResultId());
            return saved;
          });
    } catch (RuntimeException e) {
      new AttachmentStorage().deleteOwn(stored[0]);
      throw e;
    }
  }

  public Map<String, Object> labWorkspace(String keyword, String status, Integer selected) {
    Actors.require(actor, "LAB_TECHNICIAN");
    Checks.require(
        Set.of("", "PENDING", "COMPLETED", "CANCELLED").contains(status) && keyword.length() <= 100,
        "Bộ lọc không hợp lệ.");
    return Database.read(
        c -> {
          Map<String, Object> page = new LinkedHashMap<>();
          page.put("orders", new ClinicalOrderDAO().worklist(c, keyword, status));
          if (selected != null) {
            ClinicalOrder order = new ClinicalOrderDAO().findByIdRead(c, selected);
            Checks.require(order != null, "Không tìm thấy chỉ định.");
            MedicalRecord record = new MedicalRecordDAO().findByIdRead(c, order.getRecordId());
            page.put("selectedOrder", order);
            page.put("patient", new MedicalRecordDAO().patientProfile(c, record.getPatientId()));
          }
          return page;
        });
  }

  public File getAttachment(Integer resultId) {
    Actors.require(actor, "DOCTOR", "LAB_TECHNICIAN", "PATIENT");
    return Database.read(
        c -> {
          TestResult result = new TestResultDAO().findById(c, resultId);
          Checks.require(result != null, "Không tìm thấy kết quả.");
          ClinicalOrder order = new ClinicalOrderDAO().findByIdRead(c, result.getOrderId());
          MedicalRecord record = new MedicalRecordDAO().findByIdRead(c, order.getRecordId());
          Checks.require(
              actor.is("LAB_TECHNICIAN")
                  || (actor.is("DOCTOR") && record.getDoctorId().longValue() == actor.getUserId())
                  || (actor.is("PATIENT")
                      && record.getPatientId().longValue() == actor.getUserId()),
              "Bạn không có quyền tải tệp này.");
          return new AttachmentStorage().resolve(result.getFileUrl());
        });
  }

  public void createClinicalOrders(
      AuthUser doctor, long record, long[] services, String instructions) {
    new ClinicalService(doctor)
        .createClinicalOrders(
            new ClinicalOrderRequest(
                Actors.id(record),
                Arrays.stream(services).mapToObj(Actors::id).toList(),
                instructions));
  }

  public void recordTestResult(
      AuthUser tech, long order, String data, String conclusion, String url) {
    var result = new TestResult();
    result.setResultData(data);
    result.setConclusion(conclusion);
    result.setFileUrl(url);
    new ClinicalService(tech).recordTestResult(Actors.id(order), result);
  }

  public File printClinicalOrder(Integer id) {
    Actors.require(actor, "DOCTOR");
    var rows =
        Database.read(
            c -> {
              var order = new ClinicalOrderDAO().findByIdRead(c, id);
              Checks.require(order != null, "Không tìm thấy chỉ định.");
              var record =
                  EMRService.editable(
                      new MedicalRecordDAO().findByIdRead(c, order.getRecordId()), actor, false);
              var patient = new MedicalRecordDAO().patientProfile(c, record.getPatientId());
              Map<String, Object> fields = new LinkedHashMap<>();
              fields.put("Bệnh nhân", patient.get("full_name"));
              fields.put("Bác sĩ", new MedicalRecordDAO().doctorName(c, record.getDoctorId()));
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
