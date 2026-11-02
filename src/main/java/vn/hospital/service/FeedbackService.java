package vn.hospital.service;

import java.time.*;
import java.util.List;
import java.util.Map;
import vn.hospital.dao.*;
import vn.hospital.dao.FeedbackDAO;
import vn.hospital.util.*;

public class FeedbackService {
  /**
   * SQ05 - Đánh giá lượt khám. Đầu vào: patientId của phiên, recordId, điểm 1-5 và nhận xét. Chỉ
   * nhận bệnh án của chính bệnh nhân, đã CLOSED, trong 7 ngày và chưa có đánh giá. Đầu ra: ghi
   * feedback và tính lại điểm trung bình doctors trong cùng transaction.
   */
  public void submitFeedback(long patient, long record, int rating, String comment) {
    Checks.require(rating >= 1 && rating <= 5, "Số sao phải từ 1 đến 5.");
    Checks.require(comment.length() <= 2000, "Nhận xét tối đa 2000 ký tự.");
    Database.tx(
        connection -> {
          var medicalRecord = new FeedbackDAO().lockRecordRows(connection, record);
          Checks.require(
              medicalRecord != null
                  && Rows.number(medicalRecord, "patient_id") == patient
                  && "CLOSED".equals(medicalRecord.get("status")),
              "Chỉ được đánh giá bệnh án của bạn sau khi khám xong.");
          Checks.require(
              ((java.sql.Timestamp) medicalRecord.get("closed_at"))
                  .toLocalDateTime()
                  .plusDays(7)
                  .isAfter(LocalDateTime.now()),
              "Đã hết thời hạn đánh giá 7 ngày.");
          Checks.require(
              new FeedbackDAO().findExistingRows(connection, record) == null,
              "Bệnh án này đã được đánh giá.");
          new DoctorDAO().lockDoctor(connection, medicalRecord.get("doctor_id"));
          long id =
              new FeedbackDAO()
                  .insertFeedbackRow(
                      connection, record, patient, medicalRecord.get("doctor_id"), rating, comment);
          new DoctorDAO()
              .recalculateRating(
                  connection, medicalRecord.get("doctor_id"), medicalRecord.get("doctor_id"));
          AuditDAO.record(connection, patient, "SUBMIT_FEEDBACK", "feedback", id);
          return null;
        });
  }

  public List<Map<String, Object>> findEligibleFeedbackRecords(Object... values) {
    return new FeedbackDAO().findEligibleFeedbackRecords(values);
  }
}
