package vn.hospital.service;

public class ReceptionService {
  public java.util.List<java.util.Map<String, Object>> findPatients(Object... values) {
    return new vn.hospital.dao.ReceptionDAO().findPatients(values);
  }

  public java.util.List<java.util.Map<String, Object>> findAppointments(Object... values) {
    return new vn.hospital.dao.ReceptionDAO().findAppointments(values);
  }

  public long checkIn(vn.hospital.model.AuthUser actor, long appointment, String priority) {
    return new QueueService().checkIn(actor, appointment, priority);
  }

  public long registerWalkIn(
      vn.hospital.model.AuthUser actor,
      long patient,
      long schedule,
      String symptoms,
      String priority) {
    return new QueueService().registerWalkIn(actor, patient, schedule, symptoms, priority);
  }
}
