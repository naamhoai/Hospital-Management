package vn.hospital.dao;

import java.sql.*;
import java.util.List;
import vn.hospital.model.ResultRecallRequest;
import vn.hospital.util.Actors;

public class ResultRecallDAO {
  private static final String DETAILS =
      "SELECT rr.*,t.order_id,t.result_data,t.conclusion,t.file_url"
          + " original_file_url,o.record_id,m.doctor_id,s.department_id,patient.full_name"
          + " patient_name,s.name service_name,d.name department_name,requester.full_name"
          + " requester_name,reviewer.full_name reviewer_name FROM result_recall_request rr JOIN"
          + " test_result t ON t.result_id=rr.result_id JOIN clinical_order o ON"
          + " o.order_id=t.order_id JOIN medical_record m ON m.record_id=o.record_id JOIN service s"
          + " ON s.service_id=o.service_id JOIN departments d ON d.department_id=s.department_id"
          + " JOIN users patient ON patient.user_id=m.patient_id JOIN users requester ON"
          + " requester.user_id=rr.requested_by LEFT JOIN users reviewer ON"
          + " reviewer.user_id=rr.reviewed_by";

  public Integer headDepartment(Connection c, long userId) throws SQLException {
    var row =
        Jdbc.one(
            c,
            "SELECT d.department_id FROM doctors d JOIN users u ON u.user_id=d.doctor_id JOIN roles"
                + " r ON r.role_id=u.role_id WHERE d.doctor_id=? AND u.status='ACTIVE' AND"
                + " r.name='DEPARTMENT_HEAD'",
            userId);
    return row == null ? null : ((Number) row.get("department_id")).intValue();
  }

  public ResultRecallRequest findById(Connection c, int id) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(c, DETAILS + " WHERE rr.request_id=?", id), ResultRecallRequest.class);
  }

  public List<ResultRecallRequest> findByDepartment(Connection c, int department)
      throws SQLException {
    return BeanMapper.list(
        Jdbc.list(
            c,
            DETAILS
                + " WHERE s.department_id=? ORDER BY CASE rr.status WHEN 'PENDING' THEN 0 ELSE 1"
                + " END,rr.request_id DESC LIMIT 100",
            department),
        ResultRecallRequest.class);
  }

  public List<ResultRecallRequest> findByOrder(Connection c, int orderId) throws SQLException {
    return BeanMapper.list(
        Jdbc.list(c, DETAILS + " WHERE t.order_id=? ORDER BY rr.request_id DESC", orderId),
        ResultRecallRequest.class);
  }

  public boolean hasPending(Connection c, int resultId) throws SQLException {
    return Jdbc.one(
            c,
            "SELECT request_id FROM result_recall_request WHERE result_id=? AND status='PENDING'",
            resultId)
        != null;
  }

  public int create(Connection c, int resultId, long requester, String reason) throws SQLException {
    try {
      return Actors.id(
          Jdbc.insert(
              c,
              "INSERT INTO result_recall_request(result_id,requested_by,reason) VALUES(?,?,?)",
              resultId,
              requester,
              reason));
    } catch (SQLIntegrityConstraintViolationException error) {
      // UNIQUE cũng bảo vệ trường hợp hai đề nghị cùng đến trước khi giao diện kịp cập nhật.
      if (error.getErrorCode() == 1062)
        throw new vn.hospital.util.BusinessException("Kết quả đã có đề nghị đang chờ duyệt.");
      throw error;
    }
  }

  public boolean review(
      Connection c, int id, long reviewer, String status, String note, String proof)
      throws SQLException {
    return Jdbc.update(
            c,
            "UPDATE result_recall_request SET"
                + " status=?,reviewed_by=?,reviewed_at=NOW(),review_note=?,approval_file_url=?"
                + " WHERE request_id=? AND status='PENDING'",
            status,
            reviewer,
            note,
            proof,
            id)
        == 1;
  }
}
