package vn.hospital.dev;

import java.io.File;
import org.apache.catalina.startup.Tomcat;

/** Used only by the Maven dev profile; production deploys the WAR to Tomcat. */
public class DevServer {
  public static void main(String[] args) throws Exception {
    System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
    Tomcat server = new Tomcat();
    server.setPort(Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "8080")));
    server.setBaseDir(new File("target/tomcat").getAbsolutePath());
    server.getConnector().setProperty("address", "127.0.0.1");
    var context = server.addWebapp("/hospital", new File("target/hospital").getAbsolutePath());
    context.setParentClassLoader(DevServer.class.getClassLoader());
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
