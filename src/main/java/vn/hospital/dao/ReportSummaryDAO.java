package vn.hospital.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.Map;
import vn.hospital.util.Database;

/** Bảng tổng hợp theo ngày do tác vụ nền ghi; màn hình vẫn truy vấn số liệu thời gian thực. */
public class ReportSummaryDAO {
  public void saveDailySummary(LocalDate date) {
    Database.tx(
        c -> {
          String sql =
              "INSERT INTO"
                  + " report_daily_summary(summary_date,appointment_count,completed_count,cancelled_count,no_show_count,paid_invoice_count,total_revenue)"
                  + " SELECT"
                  + " ?,COUNT(*),COALESCE(SUM(status='COMPLETED'),0),COALESCE(SUM(status='CANCELLED'),0),COALESCE(SUM(status='NO_SHOW'),0),(SELECT"
                  + " COUNT(*) FROM invoice WHERE status='PAID' AND DATE(paid_at)=?),(SELECT"
                  + " COALESCE(SUM(total_amount),0) FROM invoice WHERE status='PAID' AND"
                  + " DATE(paid_at)=?) FROM appointment WHERE appointment_date=? ON DUPLICATE KEY"
                  + " UPDATE"
                  + " appointment_count=VALUES(appointment_count),completed_count=VALUES(completed_count),cancelled_count=VALUES(cancelled_count),no_show_count=VALUES(no_show_count),paid_invoice_count=VALUES(paid_invoice_count),total_revenue=VALUES(total_revenue),generated_at=NOW()";
          Jdbc.update(c, sql, date, date, date, date);
          return null;
        });
  }

  public Map<String, Object> findByDate(LocalDate date) {
    return Database.read(
        c -> Jdbc.one(c, "SELECT * FROM report_daily_summary WHERE summary_date=?", date));
  }
}
