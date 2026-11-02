package vn.hospital.service;

import java.io.*;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.integration.PharmacyClient;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class PrescriptionService {
  private final AuthUser actor;

  public PrescriptionService(AuthUser actor) {
    this.actor = actor;
  }

  /**
   * SQ17 - Tra thuốc cho form kê đơn. Đầu vào từ khóa tên/hoạt chất; trả danh sách thuốc hoạt động.
   * Chỉ đọc danh mục, không trừ kho ở bước tìm kiếm.
   */
  public List<Medication> searchMedication(String keyword) {
    Actors.require(actor, "DOCTOR");
    Checks.require(keyword.length() <= 100, "Từ khóa tối đa 100 ký tự.");
    return new MedicationDAO().searchApproved(keyword);
  }

  /** Mỗi thuốc chỉ xuất hiện một lần; số lượng và liều dùng phải được nhập hợp lệ. */
  private List<PrescriptionDetail> validateItems(PrescriptionRequest request) {
    Checks.require(request != null && request.getRecordId() != null, "Thiếu mã bệnh án.");
    Checks.require(
        request.getItems() != null
            && !request.getItems().isEmpty()
            && request.getItems().size() <= 30,
        "Đơn thuốc cần từ 1 đến 30 dòng.");
    Checks.require(
        request.getAdvice() == null || request.getAdvice().length() <= 255,
        "Lời dặn tối đa 255 ký tự.");
    var ids = new HashSet<Integer>();
    var items = new ArrayList<>(request.getItems());
    for (var item : items) {
      Checks.require(
          item != null
              && item.getMedicationId() != null
              && item.getMedicationId() > 0
              && ids.add(item.getMedicationId()),
          "Thuốc bị trùng hoặc không hợp lệ.");
      Checks.require(
          item.getQuantity() != null && item.getQuantity() > 0 && item.getQuantity() <= 1000,
          "Số lượng thuốc từ 1 đến 1000.");
      item.setDosage(Checks.text(item.getDosage(), 100, "Liều dùng"));
    }
    items.sort(Comparator.comparing(PrescriptionDetail::getMedicationId));
    return items;
  }

  /**
   * SQ17 - Kiểm tra đơn trước khi phát hành. Đầu vào: PrescriptionRequest với recordId, các dòng
   * medicationId/quantity/dosage và lời dặn. Kiểm tra đúng bác sĩ, bệnh án OPEN, có chẩn đoán, đủ
   * kết quả và tồn kho. Đầu ra Boolean; chưa lưu đơn hoặc trừ kho tại bước này.
   */
  public Boolean validatePrescription(PrescriptionRequest request) {
    Actors.require(actor, "DOCTOR");
    List<PrescriptionDetail> items = validateItems(request);
    return Database.tx(
        connection -> {
          var record =
              EMRService.editable(
                  new MedicalRecordDAO().findById(connection, request.getRecordId()), actor, true);
          Checks.require(
              record.getDiagnosis() != null && !record.getDiagnosis().isBlank(),
              "Phải lưu chẩn đoán trước khi kê đơn.");
          Checks.require(
              !new ClinicalOrderDAO().hasUnfinished(connection, record.getRecordId()),
              "Cần có đủ kết quả cận lâm sàng hoặc hủy chỉ định chưa thực hiện trước khi kê đơn.");
          return new MedicationDAO().checkStock(connection, items);
        });
  }

  /**
   * SQ17 - Phát hành đơn thuốc. Đầu vào: request đã nhập từ JSP và bác sĩ của phiên. Kiểm tra lại
   * dữ liệu trong transaction. Khóa từng thuốc theo thứ tự ID, chụp đơn giá, lưu
   * prescription/detail và trừ tồn kho. Đầu ra: Prescription có ID và các dòng thuốc. Thiếu một
   * thuốc thì rollback toàn bộ đơn/kho.
   */
  public Prescription issuePrescription(PrescriptionRequest request) {
    Actors.require(actor, "DOCTOR");
    List<PrescriptionDetail> items = validateItems(request);
    Checks.require(validatePrescription(request), "Thuốc không khả dụng hoặc không đủ tồn kho.");
    var pharmacy = new PharmacyClient();
    if (pharmacy.isConfigured()) {
      List<String> warnings = pharmacy.checkInteractions(items);
      Checks.require(
          warnings.isEmpty() || Boolean.TRUE.equals(request.getOverrideInteractions()),
          "Cảnh báo dược: "
              + String.join("; ", warnings)
              + ". Cần bác sĩ xác nhận trước khi tiếp tục.");
    }
    Prescription saved =
        Database.tx(
            connection -> {
              MedicalRecord record =
                  EMRService.editable(
                      new MedicalRecordDAO().findById(connection, request.getRecordId()),
                      actor,
                      true);
              Checks.require(
                  record.getDiagnosis() != null && !record.getDiagnosis().isBlank(),
                  "Phải lưu chẩn đoán trước khi kê đơn.");
              Checks.require(
                  !new ClinicalOrderDAO().hasUnfinished(connection, record.getRecordId()),
                  "Cần có đủ kết quả cận lâm sàng trước khi kê đơn.");
              var meds = new MedicationDAO();
              for (var item : items) {
                Medication med = meds.findById(connection, item.getMedicationId());
                Checks.require(
                    med != null && med.getStockQuantity() >= item.getQuantity(),
                    "Thuốc không khả dụng hoặc không đủ tồn kho: #" + item.getMedicationId());
                item.setMedication(med);
                item.setUnitPrice(med.getUnitPrice());
              }
              var rx = new Prescription();
              rx.setRecordId(record.getRecordId());
              rx.setAdvice(request.getAdvice());
              Prescription result = new PrescriptionDAO().saveWithDetails(connection, rx, items);
              for (var item : items)
                Checks.require(
                    meds.deductStock(connection, item), "Tồn kho đã thay đổi. Vui lòng thử lại.");
              AuditDAO.record(
                  connection,
                  actor.getUserId(),
                  "ISSUE_PRESCRIPTION",
                  "prescription",
                  result.getPrescriptionId());
              return result;
            });
    // Gửi sang hệ thống dược sau commit. Lỗi mạng không được tạo lại đơn hoặc trừ kho lần hai.
    if (pharmacy.isConfigured())
      try {
        pharmacy.syncPrescription(saved.getPrescriptionId());
      } catch (BusinessException error) {
        java.util.logging.Logger.getLogger(PrescriptionService.class.getName())
            .warning(
                "Prescription #"
                    + saved.getPrescriptionId()
                    + " saved locally; pharmacy sync failed: "
                    + error.getMessage());
      }
    return saved;
  }

  public Map<String, Object> prescriptionPage(Integer id) {
    Actors.require(actor, "DOCTOR", "PATIENT");
    return Database.read(
        connection -> {
          Prescription rx = new PrescriptionDAO().findById(connection, id);
          Checks.require(rx != null, "Không tìm thấy đơn thuốc.");
          MedicalRecord record = new MedicalRecordDAO().findByIdRead(connection, rx.getRecordId());
          Checks.require(
              record != null
                  && (actor.is("PATIENT")
                      ? record.getPatientId().longValue() == actor.getUserId()
                      : record.getDoctorId().longValue() == actor.getUserId()),
              "Bạn không được xem đơn thuốc này.");
          Map<String, Object> page = new LinkedHashMap<>();
          page.put("prescription", rx);
          page.put(
              "doctorName", new MedicalRecordDAO().doctorName(connection, record.getDoctorId()));
          page.put("record", record);
          page.put(
              "patient", new MedicalRecordDAO().patientProfile(connection, record.getPatientId()));
          return page;
        });
  }

  /**
   * SQ17 - Xuất đơn đã lưu thành PDF. Đầu vào: prescriptionId. Kiểm tra quyền bác sĩ hoặc bệnh nhân
   * sở hữu đơn. Đầu ra: file PDF tạm có thông tin đơn/thuốc; Controller gửi tải xuống rồi xóa file
   * tạm.
   */
  public File printPrescription(Integer id) {
    Map<String, Object> page = prescriptionPage(id);
    Prescription rx = (Prescription) page.get("prescription");
    MedicalRecord record = (MedicalRecord) page.get("record");
    @SuppressWarnings("unchecked")
    Map<String, Object> patient = (Map<String, Object>) page.get("patient");
    List<Map<String, Object>> rows = new ArrayList<>();
    rows.add(
        Map.of(
            "Bệnh nhân",
            patient.get("full_name"),
            "Bệnh án",
            record.getRecordId(),
            "Chẩn đoán",
            record.getDiagnosis()));
    for (var line : rx.getItems()) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("Thuốc", line.getMedication().getName());
      row.put("Số lượng", line.getQuantity());
      row.put("Đơn vị", line.getMedication().getUnit());
      row.put("Liều dùng", line.getDosage());
      rows.add(row);
    }
    rows.add(Map.of("Lời dặn", rx.getAdvice() == null ? "" : rx.getAdvice()));
    return new ReportExportService().createPDF("ĐƠN THUỐC #" + id, rows);
  }
}
