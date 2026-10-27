package vn.hospital.dao;

public class ReceptionDAO {
  public java.util.List<java.util.Map<String, Object>> findPatients(Object... values) {
    return Jdbc.read(
        "SELECT p.patient_id,u.full_name,u.email FROM patients p JOIN users u ON"
            + " u.user_id=p.patient_id ORDER BY u.full_name",
        values);
  }

  public java.util.List<java.util.Map<String, Object>> findAppointments(Object... values) {
    return Jdbc.read(
        "SELECT a.*,u.full_name patient_name,u.phone_number FROM appointment a JOIN users u ON"
            + " u.user_id=a.patient_id WHERE appointment_date=CURDATE() AND (a.appointment_code"
            + " LIKE ? OR u.phone_number LIKE ? OR u.full_name LIKE ?) ORDER BY appointment_time",
        values);
  }
}
