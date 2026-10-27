package vn.hospital.service;

import java.time.*;
import vn.hospital.dao.*;
import vn.hospital.util.*;

public class FeedbackService {
  public void submitFeedback(long patient, long record, int rating, String comment) {
    Checks.require(rating >= 1 && rating <= 5, "Số sao phải từ 1 đến 5.");
    Checks.require(comment.length() <= 2000, "Nhận xét tối đa 2000 ký tự.");
    Database.tx(
        c -> {
          var m = new FeedbackDAO().lockRecordRows(c, record);
          Checks.require(
              m != null
                  && Rows.number(m, "patient_id") == patient
                  && "CLOSED".equals(m.get("status")),
              "Chỉ được đánh giá bệnh án của bạn sau khi khám xong.");
          Checks.require(
              ((java.sql.Timestamp) m.get("closed_at"))
                  .toLocalDateTime()
                  .plusDays(7)
                  .isAfter(LocalDateTime.now()),
              "Đã hết thời hạn đánh giá 7 ngày.");
          Checks.require(
              new FeedbackDAO().findExistingRows(c, record) == null,
              "Bệnh án này đã được đánh giá.");
          new DoctorDAO().lockDoctor(c, m.get("doctor_id"));
          long id =
              new FeedbackDAO()
                  .insertFeedbackRow(c, record, patient, m.get("doctor_id"), rating, comment);
          new DoctorDAO().recalculateRating(c, m.get("doctor_id"), m.get("doctor_id"));
          AuditDAO.record(c, patient, "SUBMIT_FEEDBACK", "feedback", id);
          return null;
        });
  }

  public java.util.List<java.util.Map<String, Object>> findEligibleFeedbackRecords(
      Object... values) {
    return new vn.hospital.dao.FeedbackDAO().findEligibleFeedbackRecords(values);
  }
}
