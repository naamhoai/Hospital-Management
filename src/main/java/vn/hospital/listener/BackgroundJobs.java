package vn.hospital.listener;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.*;
import vn.hospital.service.MaintenanceService;

@WebListener
public class BackgroundJobs implements ServletContextListener {
  private ScheduledExecutorService executor;
  private final MaintenanceService maintenance = new MaintenanceService();

  public void contextInitialized(ServletContextEvent event) {
    executor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "hospital-outbox");
              t.setDaemon(true);
              return t;
            });
    executor.scheduleWithFixedDelay(
        () -> {
          try {
            maintenance.run();
          } catch (Exception e) {
            event.getServletContext().log("Background job could not run", e);
          }
        },
        10,
        60,
        TimeUnit.SECONDS);
  }

  public void contextDestroyed(ServletContextEvent event) {
    if (executor != null) executor.shutdownNow();
  }
}
