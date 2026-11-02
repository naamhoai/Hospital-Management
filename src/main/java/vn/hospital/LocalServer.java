package vn.hospital;

import java.io.File;
import org.apache.catalina.startup.Tomcat;

/** Máy chủ chạy thử bằng Maven; triển khai chính thức dùng file WAR trên Tomcat. */
public class LocalServer {
  public static void main(String[] args) throws Exception {
    System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
    // Kiểm tra DB trước khi mở web, để người mới thấy ngay thiếu schema hoặc sai cấu hình.
    try {
      vn.hospital.util.Database.read(
          connection -> {
            try (var query = connection.createStatement()) {
              query.executeQuery("SELECT user_id FROM users LIMIT 1").close();
            }
            return null;
          });
    } catch (RuntimeException error) {
      throw new IllegalStateException(
          "Chưa kết nối được database. Chạy database/hospital.sql, rồi sửa"
              + " application-local.properties theo README.",
          error);
    }
    Tomcat server = new Tomcat();
    server.setPort(Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "3000")));
    server.setBaseDir(new File("target/tomcat").getAbsolutePath());
    server.getConnector().setProperty("address", "127.0.0.1");
    var context = server.addWebapp("/hospital", new File("target/hospital").getAbsolutePath());
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
    System.out.println(
        "Hospital: http://localhost:" + server.getConnector().getPort() + "/hospital");
    server.getServer().await();
  }
}
