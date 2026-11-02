package vn.hospital.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import vn.hospital.dao.*;
import vn.hospital.model.*;
import vn.hospital.util.*;

/** Thu hồi có phê duyệt; không sửa hoặc xóa nội dung kết quả đã duyệt trước đó. */
public class ResultRecallService {
  private final AuthUser actor;
  private final ResultRecallDAO recalls = new ResultRecallDAO();
  private final TestResultDAO results = new TestResultDAO();
  private final ClinicalOrderDAO orders = new ClinicalOrderDAO();
  private final MedicalRecordDAO records = new MedicalRecordDAO();

  public ResultRecallService(AuthUser actor) {
    this.actor = actor;
  }

  /** Bác sĩ phụ trách hoặc kỹ thuật viên lập đề nghị, kèm lý do bắt buộc. */
  public int requestRecall(int resultId, String reason) {
    Actors.require(actor, "DOCTOR", "LAB_TECHNICIAN");
    String validReason = Checks.text(reason, 255, "Lý do thu hồi");
    return Database.tx(
        connection -> {
          ClinicalOrder order = lockOrderForResult(connection, resultId);
          MedicalRecord record = records.findByIdRead(connection, order.getRecordId());
          if (actor.is("DOCTOR"))
            Checks.require(
                record.getDoctorId().longValue() == actor.getUserId(), "Bệnh án không thuộc bạn.");
          TestResult result = results.findByIdForUpdate(connection, resultId);
          Checks.require(
              result != null && "APPROVED".equals(result.getStatus()),
              "Chỉ đề nghị thu hồi kết quả đang có hiệu lực.");
          Checks.require(
              !recalls.hasPending(connection, resultId), "Kết quả đã có đề nghị đang chờ duyệt.");
          int requestId = recalls.create(connection, resultId, actor.getUserId(), validReason);
          AuditDAO.record(
              connection, actor.getUserId(), "REQUEST_RESULT_RECALL", "test_result", resultId);
          return requestId;
        });
  }

  public List<ResultRecallRequest> listRequests() {
    Actors.require(actor, "DEPARTMENT_HEAD");
    return Database.read(
        connection -> recalls.findByDepartment(connection, requireHeadDepartment(connection)));
  }

  /** Chỉ trưởng khoa của dịch vụ xét nghiệm được phê duyệt; biên bản PDF được lưu cùng phiếu. */
  public void reviewRequest(int requestId, boolean approve, String note, UploadData signedReport) {
    Actors.require(actor, "DEPARTMENT_HEAD");
    String validNote = Checks.text(note, 1000, "Ý kiến phê duyệt");
    if (approve) validateSignedReport(signedReport);
    AttachmentStorage storage = new AttachmentStorage();
    String proof = approve ? storage.save(signedReport) : null;
    try {
      Database.tx(
          connection -> {
            ResultRecallRequest request = recalls.findById(connection, requestId);
            Checks.require(request != null, "Không tìm thấy đề nghị.");
            ClinicalOrder order = lockOrderForResult(connection, request.getResultId());
            // Đọc lại sau khi khóa để hai người không thể duyệt cùng một phiếu.
            request = recalls.findById(connection, requestId);
            requireReviewPermission(connection, request);
            Checks.require("PENDING".equals(request.getStatus()), "Đề nghị đã được xử lý.");
            Checks.require(
                request.getRequestedBy().longValue() != actor.getUserId(),
                "Không tự phê duyệt đề nghị của mình.");
            TestResult original = results.findByIdForUpdate(connection, request.getResultId());
            Checks.require(
                "APPROVED".equals(original.getStatus()),
                "Kết quả không còn hiệu lực để xử lý đề nghị.");
            String decision = approve ? "APPROVED" : "REJECTED";
            Checks.require(
                recalls.review(
                    connection, requestId, actor.getUserId(), decision, validNote, proof),
                "Đề nghị đã được xử lý.");
            if (approve) {
              // Giữ bản gốc; lần nhập tiếp theo tạo một TestResult mới cho cùng chỉ định.
              results.markRecalled(connection, original.getResultId());
              orders.updateExecutionStatus(connection, order.getOrderId(), "RESULT_RECALLED");
              MedicalRecord record = records.findByIdRead(connection, order.getRecordId());
              AppointmentService.notify(
                  connection,
                  record.getDoctorId(),
                  "Kết quả đã được thu hồi",
                  "Chỉ định #"
                      + order.getOrderId()
                      + " cần cập nhật kết quả. Vui lòng kiểm tra lại thông tin liên quan.");
              AppointmentService.notify(
                  connection,
                  record.getPatientId(),
                  "Kết quả đang được cập nhật",
                  "Kết quả chỉ định #" + order.getOrderId() + " đã thu hồi để kiểm tra, cập nhật.");
              AuditDAO.record(
                  connection,
                  actor.getUserId(),
                  "RECALL_RESULT",
                  "test_result",
                  original.getResultId());
            }
            AuditDAO.record(
                connection,
                actor.getUserId(),
                approve ? "APPROVE_RECALL" : "REJECT_RECALL",
                "result_recall_request",
                requestId);
            return null;
          });
    } catch (RuntimeException error) {
      storage.deleteOwn(proof);
      throw error;
    }
  }

  public File getApprovalFile(int requestId) {
    Actors.require(actor, "DEPARTMENT_HEAD");
    return Database.read(
        connection -> {
          ResultRecallRequest request = recalls.findById(connection, requestId);
          Checks.require(request != null, "Không tìm thấy phiếu.");
          requireReviewPermission(connection, request);
          return new AttachmentStorage().resolve(request.getApprovalFileUrl());
        });
  }

  private ClinicalOrder lockOrderForResult(Connection connection, int resultId)
      throws SQLException {
    Integer orderId = results.findOrderId(connection, resultId);
    Checks.require(orderId != null, "Không tìm thấy kết quả.");
    Integer recordId = orders.findRecordId(connection, orderId);
    records.findById(connection, recordId); // Thứ tự khóa thống nhất: bệnh án -> chỉ định.
    return orders.findById(connection, orderId);
  }

  private int requireHeadDepartment(Connection connection) throws SQLException {
    Integer department = recalls.headDepartment(connection, actor.getUserId());
    Checks.require(department != null, "Tài khoản trưởng khoa chưa được phân công chuyên khoa.");
    return department;
  }

  private void requireReviewPermission(Connection connection, ResultRecallRequest request)
      throws SQLException {
    Checks.require(
        request.getDepartmentId() == requireHeadDepartment(connection),
        "Chỉ được xử lý kết quả thuộc chuyên khoa phụ trách.");
  }

  private void validateSignedReport(UploadData report) {
    Checks.require(
        report != null && report.bytes().length >= 5,
        "Cần tải biên bản thu hồi có chữ ký dạng PDF.");
    String header = new String(report.bytes(), 0, 5, StandardCharsets.US_ASCII);
    Checks.require("%PDF-".equals(header), "Biên bản phê duyệt phải là tệp PDF.");
  }

  /** Trưởng khoa được tra cứu tệp gốc qua phiếu; người bệnh chỉ tải bản đang có hiệu lực. */
  public File getOriginalResultFile(int requestId) {
    Actors.require(actor, "DEPARTMENT_HEAD");
    return Database.read(
        connection -> {
          ResultRecallRequest request = recalls.findById(connection, requestId);
          Checks.require(request != null, "Không tìm thấy phiếu.");
          requireReviewPermission(connection, request);
          return new AttachmentStorage().resolve(request.getOriginalFileUrl());
        });
  }
}
