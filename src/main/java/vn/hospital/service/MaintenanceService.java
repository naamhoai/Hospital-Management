package vn.hospital.service;

import vn.hospital.dao.*;
import vn.hospital.integration.EmailClient;
import vn.hospital.util.Database;

public class MaintenanceService {
  public void run() {
    Database.tx(
        c -> {
          new AppointmentDAO().markNoShows(c);
          return null;
        });
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
          c -> {
            dao.recordDelivery(c, row.get("id"), ok);
            return null;
          });
    }
  }
}
