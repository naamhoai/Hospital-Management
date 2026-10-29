package vn.hospital.model;

import java.util.List;

/** Tiến trình khám được suy ra từ dữ liệu hiện có, không sửa trạng thái khóa OPEN/CLOSED. */
public enum ClinicalProgress {
  NOT_DIAGNOSED("Chưa chẩn đoán"),
  EXAMINED("Đã chẩn đoán"),
  WAITING_TESTS("Chờ kết quả xét nghiệm"),
  RESULTS_READY("Đã đủ kết quả"),
  FINISHED("Đã kết thúc");

  private final String label;

  ClinicalProgress(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static ClinicalProgress from(MedicalRecord record, List<ClinicalOrder> orders) {
    // Kết quả bị thu hồi phải hiện cảnh báo chờ cập nhật, kể cả lượt khám đã đóng.
    for (ClinicalOrder order : orders) {
      if ("RESULT_RECALLED".equals(order.getStatus())) return WAITING_TESTS;
    }
    if ("CLOSED".equals(record.getStatus())) return FINISHED;
    if (record.getDiagnosis() == null || record.getDiagnosis().isBlank()) return NOT_DIAGNOSED;
    boolean hasCompletedTest = false;
    for (ClinicalOrder order : orders) {
      if ("CANCELLED".equals(order.getStatus())) continue;
      if (!"COMPLETED".equals(order.getStatus())) return WAITING_TESTS;
      hasCompletedTest = true;
    }
    return hasCompletedTest ? RESULTS_READY : EXAMINED;
  }
}
