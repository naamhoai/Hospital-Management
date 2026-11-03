package vn.hospital;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.catalina.startup.Tomcat;

/** Máy chủ dùng chung cho nhóm: Tomcat trên http://localhost:3000/hospital. */
public class LocalServer {
  private static final int PORT = 3000;
  private static final String CONTEXT_PATH = "/hospital";

  public static void main(String[] args) throws Exception {
    System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
    File webRoot = new File("target/hospital").getAbsoluteFile();
    if (!webRoot.isDirectory()) {
      throw new IllegalStateException(
          "Chưa có thư mục target/hospital. Chạy scripts/run-dev.ps1 để build và mở web cổng 3000.");
    }
    // Kiểm tra DB trước khi mở web, để người mới thấy ngay thiếu schema hoặc sai cấu hình.
    try {
      vn.hospital.util.Database.read(
          connection -> {
            checkDatabaseSchema(connection);
            return null;
          });
    } catch (RuntimeException error) {
      throw new IllegalStateException(
          "Database chưa sẵn sàng. Kiểm tra MySQL và DB_URL/DB_USER/DB_PASSWORD trong"
              + " application-local.properties. Cần database hospital đủ cấu trúc theo"
              + " database/hospital.sql; chỉ nạp SQL vào database chưa có bảng. Xem nguyên nhân bên dưới.",
          error);
    }
    Tomcat server = new Tomcat();
    // Cố định cổng cho cả nhóm; không phụ thuộc HTTP_PORT riêng trên từng máy.
    server.setPort(PORT);
    server.setBaseDir(new File("target/tomcat").getAbsolutePath());
    server.getConnector().setProperty("address", "127.0.0.1");
    var context = server.addWebapp(CONTEXT_PATH, webRoot.getAbsolutePath());
    context.setParentClassLoader(LocalServer.class.getClassLoader());
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    server.stop();
                    server.destroy();
                  } catch (Exception ignored) {
                  }
                }));
    server.start();
    if (!context.getState().isAvailable()) {
      server.stop();
      throw new IllegalStateException("Web application failed to start; inspect Tomcat log");
    }
    System.out.println("Hospital: http://localhost:" + PORT + CONTEXT_PATH + "/login");
    server.getServer().await();
  }

  /** Kiểm tra schema trước khi mở web; các truy vấn LIMIT 0 chỉ đọc cấu trúc, không sửa dữ liệu. */
  private static void checkDatabaseSchema(Connection connection) throws SQLException {
    if (!"hospital".equals(connection.getCatalog())) {
      throw new IllegalStateException("DB_URL phải trỏ tới database hospital theo cấu hình chung của nhóm.");
    }
    String[] tables = {
      "roles", "users", "departments", "rooms", "doctors", "patients", "work_schedule",
      "service", "appointment", "queue", "medical_record", "medication", "prescription",
      "prescription_detail", "clinical_order", "test_result", "invoice", "feedback",
      "queue_counter", "invoice_line", "payment_attempt", "notification_outbox", "audit_log",
      "result_recall_request", "report_daily_summary", "service_package_item"
    };
    try (var query = connection.createStatement()) {
      // Tên bảng là các hằng nội bộ, không lấy từ request hoặc dữ liệu người dùng.
      for (String table : tables) {
        query.executeQuery("SELECT 1 FROM `" + table + "` LIMIT 0").close();
      }
      // Những cột từng thay đổi giữa các phiên bản phải có trước khi demo các luồng chính.
      String[] columnChecks = {
        "SELECT password FROM users LIMIT 0",
        "SELECT description FROM service LIMIT 0",
        "SELECT package_service_id, unit_price FROM clinical_order LIMIT 0",
        "SELECT leave_status, leave_reason, leave_requested_at, leave_reviewed_by,"
            + " leave_reviewed_at, leave_review_note FROM work_schedule LIMIT 0",
        "SELECT status, active_order_id FROM test_result LIMIT 0",
        "SELECT package_service_id, component_service_id FROM service_package_item LIMIT 0"
      };
      for (String sql : columnChecks) query.executeQuery(sql).close();
    }
  }
}
