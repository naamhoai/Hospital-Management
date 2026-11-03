package vn.hospital.service;

import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.integration.PharmacyClient;
import vn.hospital.integration.QueueContextPort;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class EMRService {
  private final AuthUser actor;

  public EMRService(AuthUser actor) {
    this.actor = actor;
  }

  /**
   * SQ16 - Mở bệnh án theo queueId từ khối tiếp nhận. Đầu vào: queueId, bác sĩ được giữ trong
   * Service từ HttpSession. QueueContextPort tìm lượt chờ; kiểm tra đúng bác sĩ rồi
   * MedicalRecordDAO tìm qua appointmentId. Đầu ra: MedicalRecord; không cho bác sĩ tự nhập
   * patientId để mở tùy ý bệnh án.
   */
  public MedicalRecord getRecord(Integer queueId) {
    Actors.require(actor, "DOCTOR");
    return Database.tx(
        connection -> {
          QueueContext queueContext = new QueueContextPort().getContext(connection, queueId);
          Checks.require(
              queueContext != null
                  && queueContext.getDoctorId() != null
                  && queueContext.getDoctorId().longValue() == actor.getUserId(),
              "Lượt khám không được phân công cho bạn.");
          return new MedicalRecordDAO().findByQueueId(connection, queueId);
        });
  }

  /**
   * SQ16 - Lưu khám lâm sàng và chẩn đoán. Đầu vào: queueId và DiagnosisRequest gồm triệu chứng,
   * ICD, chẩn đoán, ghi chú. Kiểm tra mã ICD, bác sĩ sở hữu lượt IN_CONSULTATION và bệnh án còn
   * OPEN. Đầu ra: MedicalRecord đã lưu và audit_log; Controller hiển thị lại EMR. Sai dữ liệu trả
   * lỗi 422.
   */
  public MedicalRecord recordClinicalDiagnosis(Integer queueId, DiagnosisRequest request) {
    Actors.require(actor, "DOCTOR");
    Checks.require(request != null, "Thiếu dữ liệu chẩn đoán.");
    String symptoms = Checks.text(request.getSymptoms(), 5000, "Triệu chứng"),
        code = Checks.text(request.getIcdCode(), 20, "Mã ICD").toUpperCase(Locale.ROOT),
        diagnosis = Checks.text(request.getDiagnosis(), 255, "Chẩn đoán");
    String notes = request.getNotes() == null ? "" : request.getNotes().trim();
    Checks.require(notes.length() <= 5000, "Ghi chú tối đa 5000 ký tự.");
    Checks.require(IcdCatalog.contains(code), "Mã ICD chưa có trong danh mục được cấu hình.");
    return Database.tx(
        connection -> {
          QueueContext queueContext = new QueueContextPort().getContext(connection, queueId);
          Checks.require(
              queueContext != null
                  && queueContext.getDoctorId() != null
                  && queueContext.getDoctorId().longValue() == actor.getUserId()
                  && "IN_CONSULTATION".equals(queueContext.getStatus()),
              "Lượt khám chưa được gọi hoặc không thuộc bạn.");
          var dao = new MedicalRecordDAO();
          MedicalRecord record = editable(dao.findByQueueId(connection, queueId), actor, true);
          record.setSymptoms(symptoms);
          record.setIcdCode(code);
          record.setDiagnosis(diagnosis);
          record.setNotes(notes);
          MedicalRecord saved = dao.save(connection, record);
          AuditDAO.record(
              connection,
              actor.getUserId(),
              "SAVE_DIAGNOSIS",
              "medical_record",
              saved.getRecordId());
          return saved;
        });
  }

  /**
   * SQ16 - Đọc lịch sử bệnh nhân phục vụ khám. Đầu vào: patientId; kiểm tra bác sĩ đã được phân
   * công khám người đó. Đầu ra: danh sách bệnh án từ MedicalRecordDAO, không sửa hồ sơ lịch sử.
   */
  public List<MedicalRecord> getPatientHistory(Integer patientId) {
    Actors.require(actor, "DOCTOR");
    return Database.tx(
        connection -> {
          var dao = new MedicalRecordDAO();
          Checks.require(
              dao.assignedPatient(connection, actor.getUserId(), patientId),
              "Bạn không được xem lịch sử bệnh nhân này.");
          return dao.findByPatientId(connection, patientId);
        });
  }

  public static MedicalRecord editable(MedicalRecord record, AuthUser doctor, boolean open) {
    Actors.require(doctor, "DOCTOR");
    Checks.require(
        record != null && record.getDoctorId().longValue() == doctor.getUserId(),
        "Bạn không được truy cập bệnh án này.");
    if (open)
      Checks.require(
          "OPEN".equals(record.getStatus()), "Bệnh án đã kết thúc; không thể chỉnh sửa.");
    return record;
  }

  /**
   * WF01 - Kết thúc lượt khám sau chẩn đoán và kết quả cần thiết. Đầu vào: queueId của bác sĩ. Chặn
   * khi thiếu chẩn đoán hoặc còn chỉ định chưa hoàn tất. Đầu ra: bệnh án CLOSED, queue/appointment
   * COMPLETED và hóa đơn cho khoản chưa tính. CLOSED là hoàn tất chuyên môn, không có nghĩa mọi hóa
   * đơn đều đã PAID.
   */
  public void closeRecord(Integer queueId) {
    Actors.require(actor, "DOCTOR");
    Database.tx(
        connection -> {
          var dao = new MedicalRecordDAO();
          MedicalRecord record = editable(dao.findByQueueId(connection, queueId), actor, true);
          Checks.require(
              record.getDiagnosis() != null
                  && !record.getDiagnosis().isBlank()
                  && record.getIcdCode() != null,
              "Phải lưu chẩn đoán trước khi kết thúc khám.");
          Checks.require(
              !new ClinicalOrderDAO().hasUnfinished(connection, record.getRecordId()),
              "Vẫn còn chỉ định chưa hoàn tất. Hãy chờ kết quả hoặc hủy chỉ định chưa thực hiện.");
          new BillingService().generateFinalInvoice(connection, actor, record);
          dao.close(connection, record.getRecordId());
          dao.completeEncounter(connection, record);
          AppointmentService.notify(
              connection,
              record.getPatientId(),
              "Hoàn tất khám bệnh",
              "Bạn có thể đánh giá chất lượng khám trong 7 ngày.");
          AuditDAO.record(
              connection,
              actor.getUserId(),
              "CLOSE_RECORD",
              "medical_record",
              record.getRecordId());
          return null;
        });
  }

  public Map<String, Object> workspace(Integer queueId, String keyword) {
    Actors.require(actor, "DOCTOR");
    Checks.require(keyword.length() <= 100, "Từ khóa tối đa 100 ký tự.");
    return Database.read(
        connection -> {
          Map<String, Object> page = new LinkedHashMap<>();
          var dao = new MedicalRecordDAO();
          page.put("records", dao.findConsultations(connection, actor.getUserId(), keyword));
          page.put("icds", IcdCatalog.entries());
          page.put("pharmacyConfigured", new PharmacyClient().isConfigured());
          page.put(
              "services",
              new MedicalServiceDAO()
                  .findAllActive(connection).stream()
                      .filter(s -> !"CONSULTATION".equals(s.getCategory()))
                      .toList());
          page.put("medications", new MedicationDAO().searchApproved(connection, ""));
          page.put("prescriptionRows", List.of(Map.of("medicationId", "", "quantity", "", "dosage", "")));
          if (queueId != null) {
            QueueContext queueContext = new QueueContextPort().getContext(connection, queueId);
            Checks.require(
                queueContext != null
                    && queueContext.getDoctorId() != null
                    && queueContext.getDoctorId().longValue() == actor.getUserId(),
                "Lượt khám không thuộc bạn.");
            MedicalRecord record =
                editable(dao.findByQueueIdRead(connection, queueId), actor, false);
            page.put("queueContext", queueContext);
            page.put("record", record);
            page.put("patient", dao.patientProfile(connection, record.getPatientId()));
            page.put("history", dao.findClosedHistory(connection, record.getPatientId()));
            var orders = new ClinicalOrderDAO().findByRecordId(connection, record.getRecordId());
            page.put("orders", orders);
            page.put("clinicalProgress", ClinicalProgress.from(record, orders));
            // Chỉ hướng dẫn từ dữ liệu đã lưu, không tự xác nhận thanh toán/kết quả thay vai trò khác.
            var pendingOrders = orders.stream()
                .filter(o -> !Set.of("COMPLETED", "CANCELLED").contains(o.getStatus()))
                .toList();
            boolean hasDiagnosis = record.getDiagnosis() != null && !record.getDiagnosis().isBlank();
            page.put("hasDiagnosis", hasDiagnosis);
            page.put("hasPendingTests", !pendingOrders.isEmpty());
            page.put("pendingOrders", pendingOrders);
            page.put("hasUnpaidTests", pendingOrders.stream()
                .anyMatch(o -> !"PAID".equals(o.getPaymentStatus())));
            page.put(
                "prescriptions",
                new PrescriptionDAO().findByRecordId(connection, record.getRecordId()));
          }
          return page;
        });
  }
}
