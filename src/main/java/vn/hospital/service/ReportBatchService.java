package vn.hospital.service;

import java.time.*;
import vn.hospital.dao.ReportSummaryDAO;

public class ReportBatchService {
  private LocalDate lastSuccessfulDay;

  /** Sau nửa đêm, tổng hợp ngày trước một lần; khởi động lại cũng cập nhật cùng một dòng. */
  public synchronized void summarizePreviousDay() {
    LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
    if (yesterday.equals(lastSuccessfulDay)) return;
    new ReportSummaryDAO().saveDailySummary(yesterday);
    lastSuccessfulDay = yesterday; // Chỉ đánh dấu thành công sau khi DB đã commit.
  }
}
