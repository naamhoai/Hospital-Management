package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class AppointmentDAO {
  public Map<String, Object> findById(Connection c, long id) throws SQLException {
    return Jdbc.one(c, "SELECT * FROM appointment WHERE appointment_id=? FOR UPDATE", id);
  }

  public List<Map<String, Object>> findByPatient(long id) {
    return Jdbc.read(
        "SELECT a.*,u.full_name doctor_name FROM appointment a JOIN users u ON"
            + " a.doctor_id=u.user_id WHERE patient_id=? ORDER BY appointment_date"
            + " DESC,appointment_time",
        id);
  }

  public Map<String, Object> countMonthlyCancellationsRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT COUNT(*) n FROM appointment WHERE patient_id=? AND"
            + " cancelled_at>=DATE_FORMAT(CURDATE(),'%Y-%m-01')",
        values);
  }

  public int cancelRow(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE appointment SET status='CANCELLED',cancelled_at=NOW() WHERE appointment_id=?",
        values);
  }

  public int rescheduleRow(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE appointment SET"
            + " schedule_id=?,doctor_id=?,appointment_date=?,appointment_time=?,reschedule_count=reschedule_count+1"
            + " WHERE appointment_id=?",
        values);
  }

  public List<Map<String, Object>> findOccupiedTimes(Connection c, Object... values)
      throws SQLException {
    return Jdbc.list(
        c,
        "SELECT appointment_time FROM appointment WHERE schedule_id=? AND appointment_id<>? AND"
            + " status IN ('BOOKED','CHECKED_IN','COMPLETED') FOR UPDATE",
        values);
  }

  public long insertAppointmentRow(Connection c, Object... values) throws SQLException {
    return Jdbc.insert(
        c,
        "INSERT INTO"
            + " appointment(appointment_code,patient_id,doctor_id,schedule_id,appointment_date,appointment_time,initial_symptoms,consultation_fee)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        values);
  }

  public Map<String, Object> countIncompleteRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT COUNT(*) n FROM appointment WHERE patient_id=? AND status IN"
            + " ('BOOKED','CHECKED_IN')",
        values);
  }

  public int cancelScheduleAppointments(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE appointment SET status='CANCELLED' WHERE schedule_id=? AND status='BOOKED'",
        values);
  }

  public List<Map<String, Object>> findBookedPatients(Connection c, Object... values)
      throws SQLException {
    return Jdbc.list(
        c,
        "SELECT patient_id,appointment_id FROM appointment WHERE schedule_id=? AND status='BOOKED'",
        values);
  }

  public Map<String, Object> findReceivedPatientRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT appointment_id FROM appointment WHERE schedule_id=? AND status IN"
            + " ('CHECKED_IN','COMPLETED')",
        values);
  }

  public int markCheckedIn(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c, "UPDATE appointment SET status='CHECKED_IN' WHERE appointment_id=?", values);
  }

  public void markNoShows(Connection c) throws SQLException {
    Jdbc.update(
        c,
        "UPDATE appointment SET status='NO_SHOW' WHERE status='BOOKED' AND"
            + " appointment_date<CURDATE()");
  }

  public vn.hospital.model.Appointment findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM appointment WHERE appointment_id=?", id),
                vn.hospital.model.Appointment.class));
  }
}
