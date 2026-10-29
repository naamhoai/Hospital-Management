package vn.hospital.service;

import java.io.*;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.integration.PharmacyClient;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class PrescriptionService {
  private final AuthUser actor;

  public PrescriptionService() {
    this(null);
  }

  public PrescriptionService(AuthUser actor) {
    this.actor = actor;
  }

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

  /** Chưa được kê đơn nếu còn chờ xét nghiệm; kiểm tra tồn kho sơ bộ để báo lỗi sớm. */
  public Boolean validatePrescription(PrescriptionRequest request) {
    Actors.require(actor, "DOCTOR");
    List<PrescriptionDetail> items = validateItems(request);
    return Database.tx(
        c -> {
          var record =
              EMRService.editable(
                  new MedicalRecordDAO().findById(c, request.getRecordId()), actor, true);
          Checks.require(
              record.getDiagnosis() != null && !record.getDiagnosis().isBlank(),
              "Phải lưu chẩn đoán trước khi kê đơn.");
          Checks.require(
              !new ClinicalOrderDAO().hasUnfinished(c, record.getRecordId()),
              "Cần có đủ kết quả cận lâm sàng hoặc hủy chỉ định chưa thực hiện trước khi kê đơn.");
          return new MedicationDAO().checkStock(c, items);
        });
  }

  /** Lưu đơn, các dòng thuốc và trừ kho trong cùng transaction để không bị trừ kho một phần. */
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
            c -> {
              MedicalRecord record =
                  EMRService.editable(
                      new MedicalRecordDAO().findById(c, request.getRecordId()), actor, true);
              Checks.require(
                  record.getDiagnosis() != null && !record.getDiagnosis().isBlank(),
                  "Phải lưu chẩn đoán trước khi kê đơn.");
              Checks.require(
                  !new ClinicalOrderDAO().hasUnfinished(c, record.getRecordId()),
                  "Cần có đủ kết quả cận lâm sàng trước khi kê đơn.");
              var meds = new MedicationDAO();
              for (var item : items) {
                Medication med = meds.findById(c, item.getMedicationId());
                Checks.require(
                    med != null && med.getStockQuantity() >= item.getQuantity(),
                    "Thuốc không khả dụng hoặc không đủ tồn kho: #" + item.getMedicationId());
                item.setMedication(med);
                item.setUnitPrice(med.getUnitPrice());
              }
              var rx = new Prescription();
              rx.setRecordId(record.getRecordId());
              rx.setAdvice(request.getAdvice());
              Prescription result = new PrescriptionDAO().saveWithDetails(c, rx, items);
              for (var item : items)
                Checks.require(meds.deductStock(c, item), "Tồn kho đã thay đổi. Vui lòng thử lại.");
              AuditDAO.record(
                  c,
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
        c -> {
          Prescription rx = new PrescriptionDAO().findById(c, id);
          Checks.require(rx != null, "Không tìm thấy đơn thuốc.");
          MedicalRecord record = new MedicalRecordDAO().findByIdRead(c, rx.getRecordId());
          Checks.require(
              record != null
                  && (actor.is("PATIENT")
                      ? record.getPatientId().longValue() == actor.getUserId()
                      : record.getDoctorId().longValue() == actor.getUserId()),
              "Bạn không được xem đơn thuốc này.");
          Map<String, Object> page = new LinkedHashMap<>();
          page.put("prescription", rx);
          page.put("doctorName", new MedicalRecordDAO().doctorName(c, record.getDoctorId()));
          page.put("record", record);
          page.put("patient", new MedicalRecordDAO().patientProfile(c, record.getPatientId()));
          return page;
        });
  }

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

  public long issuePrescription(
      AuthUser doctor, long record, List<PrescriptionItem> items, String advice) {
    var request = new PrescriptionRequest();
    request.setRecordId(Actors.id(record));
    request.setAdvice(advice);
    request.setItems(
        items.stream()
            .map(
                i -> {
                  var d = new PrescriptionDetail();
                  d.setMedicationId(Actors.id(i.medicationId()));
                  d.setQuantity(i.quantity());
                  d.setDosage(i.dosage());
                  return d;
                })
            .toList());
    return new PrescriptionService(doctor).issuePrescription(request).getPrescriptionId();
  }
}
