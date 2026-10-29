package vn.hospital.service;

import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.integration.QueueContextPort;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class EMRService {
  private final AuthUser actor;

  public EMRService() {
    this(null);
  }

  public EMRService(AuthUser actor) {
    this.actor = actor;
  }

  /** Nhận queueId từ khối tiếp nhận, xác minh bác sĩ rồi lấy bệnh án tương ứng. */
  public MedicalRecord getRecord(Integer queueId) {
    Actors.require(actor, "DOCTOR");
    return Database.tx(
        c -> {
          QueueContext q = new QueueContextPort().getContext(c, queueId);
          Checks.require(
              q != null
                  && q.getDoctorId() != null
                  && q.getDoctorId().longValue() == actor.getUserId(),
              "Lượt khám không được phân công cho bạn.");
          return new MedicalRecordDAO().findByQueueId(c, queueId);
        });
  }

  /** Kiểm tra dữ liệu và quyền sở hữu trước khi lưu chẩn đoán; lỗi sẽ rollback toàn bộ. */
  public MedicalRecord recordClinicalDiagnosis(Integer queueId, DiagnosisRequest request) {
    Actors.require(actor, "DOCTOR");
    Checks.require(request != null, "Thiếu dữ liệu chẩn đoán.");
    String symptoms = Checks.text(request.getSymptoms(), 5000, "Triệu chứng"),
        code = Checks.text(request.getIcdCode(), 20, "Mã ICD"),
        diagnosis = Checks.text(request.getDiagnosis(), 255, "Chẩn đoán");
    String notes = request.getNotes() == null ? "" : request.getNotes().trim();
    Checks.require(notes.length() <= 5000, "Ghi chú tối đa 5000 ký tự.");
    Checks.require(IcdCatalog.contains(code), "Mã ICD chưa có trong danh mục được cấu hình.");
    return Database.tx(
        c -> {
          QueueContext q = new QueueContextPort().getContext(c, queueId);
          Checks.require(
              q != null
                  && q.getDoctorId() != null
                  && q.getDoctorId().longValue() == actor.getUserId()
                  && "IN_CONSULTATION".equals(q.getStatus()),
              "Lượt khám chưa được gọi hoặc không thuộc bạn.");
          var dao = new MedicalRecordDAO();
          MedicalRecord m = editable(dao.findByQueueId(c, queueId), actor, true);
          m.setSymptoms(symptoms);
          m.setIcdCode(code);
          m.setDiagnosis(diagnosis);
          m.setNotes(notes);
          MedicalRecord saved = dao.save(c, m);
          AuditDAO.record(
              c, actor.getUserId(), "SAVE_DIAGNOSIS", "medical_record", saved.getRecordId());
          return saved;
        });
  }

  public List<MedicalRecord> getPatientHistory(Integer patientId) {
    Actors.require(actor, "DOCTOR");
    return Database.tx(
        c -> {
          var dao = new MedicalRecordDAO();
          Checks.require(
              dao.assignedPatient(c, actor.getUserId(), patientId),
              "Bạn không được xem lịch sử bệnh nhân này.");
          return dao.findByPatientId(c, patientId);
        });
  }

  public static MedicalRecord editable(MedicalRecord m, AuthUser doctor, boolean open) {
    Actors.require(doctor, "DOCTOR");
    Checks.require(
        m != null && m.getDoctorId().longValue() == doctor.getUserId(),
        "Bạn không được truy cập bệnh án này.");
    if (open)
      Checks.require("OPEN".equals(m.getStatus()), "Bệnh án đã kết thúc; không thể chỉnh sửa.");
    return m;
  }

  /** Chỉ kết thúc khi có chẩn đoán và không còn chỉ định đang chờ; tạo hóa đơn cho phí mới. */
  public void closeRecord(Integer queueId) {
    Actors.require(actor, "DOCTOR");
    Database.tx(
        c -> {
          var dao = new MedicalRecordDAO();
          MedicalRecord m = editable(dao.findByQueueId(c, queueId), actor, true);
          Checks.require(
              m.getDiagnosis() != null && !m.getDiagnosis().isBlank() && m.getIcdCode() != null,
              "Phải lưu chẩn đoán trước khi kết thúc khám.");
          Checks.require(
              !new ClinicalOrderDAO().hasUnfinished(c, m.getRecordId()),
              "Vẫn còn chỉ định chưa hoàn tất. Hãy chờ kết quả hoặc hủy chỉ định chưa thực hiện.");
          new BillingService().generateFinalInvoice(c, actor, m);
          dao.close(c, m.getRecordId());
          dao.completeEncounter(c, m);
          AppointmentService.notify(
              c,
              m.getPatientId(),
              "Hoàn tất khám bệnh",
              "Bạn có thể đánh giá chất lượng khám trong 7 ngày.");
          AuditDAO.record(c, actor.getUserId(), "CLOSE_RECORD", "medical_record", m.getRecordId());
          return null;
        });
  }

  public Map<String, Object> workspace(Integer queueId, String keyword) {
    Actors.require(actor, "DOCTOR");
    Checks.require(keyword.length() <= 100, "Từ khóa tối đa 100 ký tự.");
    return Database.read(
        c -> {
          Map<String, Object> page = new LinkedHashMap<>();
          var dao = new MedicalRecordDAO();
          page.put("records", dao.findConsultations(c, actor.getUserId(), keyword));
          page.put("icds", IcdCatalog.entries());
          page.put(
              "pharmacyConfigured", new vn.hospital.integration.PharmacyClient().isConfigured());
          page.put(
              "services",
              new MedicalServiceDAO()
                  .findAllActive(c).stream()
                      .filter(s -> !"CONSULTATION".equals(s.getCategory()))
                      .toList());
          page.put("medications", new MedicationDAO().searchApproved(c, ""));
          if (queueId != null) {
            QueueContext q = new QueueContextPort().getContext(c, queueId);
            Checks.require(
                q != null
                    && q.getDoctorId() != null
                    && q.getDoctorId().longValue() == actor.getUserId(),
                "Lượt khám không thuộc bạn.");
            MedicalRecord m = editable(dao.findByQueueIdRead(c, queueId), actor, false);
            page.put("queueContext", q);
            page.put("record", m);
            page.put("patient", dao.patientProfile(c, m.getPatientId()));
            page.put("history", dao.findClosedHistory(c, m.getPatientId()));
            var orders = new ClinicalOrderDAO().findByRecordId(c, m.getRecordId());
            page.put("orders", orders);
            page.put("clinicalProgress", ClinicalProgress.from(m, orders));
            page.put(
                "hasPendingTests",
                orders.stream()
                    .anyMatch(o -> !Set.of("COMPLETED", "CANCELLED").contains(o.getStatus())));
            page.put("prescriptions", new PrescriptionDAO().findByRecordId(c, m.getRecordId()));
          }
          return page;
        });
  }

  public MedicalRecord getRecord(AuthUser doctor, long queue) {
    return new EMRService(doctor).getRecord(Actors.id(queue));
  }

  public MedicalRecord recordClinicalDiagnosis(
      AuthUser doctor, long queue, DiagnosisRequest request) {
    return new EMRService(doctor).recordClinicalDiagnosis(Actors.id(queue), request);
  }

  public void closeRecord(AuthUser doctor, long queue) {
    new EMRService(doctor).closeRecord(Actors.id(queue));
  }
}
