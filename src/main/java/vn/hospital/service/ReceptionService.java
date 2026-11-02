package vn.hospital.service;

import java.util.List;
import java.util.Map;
import vn.hospital.dao.ReceptionDAO;
import vn.hospital.model.AuthUser;

public class ReceptionService {
  public List<Map<String, Object>> findPatients(Object... values) {
    return new ReceptionDAO().findPatients(values);
  }

  public List<Map<String, Object>> findAppointments(Object... values) {
    return new ReceptionDAO().findAppointments(values);
  }

  public long checkIn(AuthUser actor, long appointment, String priority) {
    return new QueueService().checkIn(actor, appointment, priority);
  }

  public long registerWalkIn(
      AuthUser actor, long patient, long schedule, String symptoms, String priority) {
    return new QueueService().registerWalkIn(actor, patient, schedule, symptoms, priority);
  }
}
