package vn.hospital.service;

import vn.hospital.dao.*;
import vn.hospital.integration.EmailClient;
import vn.hospital.util.Database;

public class MaintenanceService {
  private final ReportBatchService reportBatch = new ReportBatchService();

  public void run() {
    Database.tx(
        connection -> {
          new AppointmentDAO().markNoShows(connection);
          return null;
        });
    reportBatch.summarizePreviousDay();
    var client = new EmailClient();
    if (!client.configured()) return;
    var dao = new NotificationDAO();
    for (var row : dao.pending()) {
      boolean sent;
      try {
        client.send(
            (String) row.get("recipient"), (String) row.get("subject"), (String) row.get("body"));
        sent = true;
      } catch (Exception e) {
        sent = false;
      }
      boolean ok = sent;
      Database.tx(
          connection -> {
            dao.recordDelivery(connection, row.get("id"), ok);
            return null;
          });
    }
  }
}
