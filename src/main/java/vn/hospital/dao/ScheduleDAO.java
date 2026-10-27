package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class ScheduleDAO {
  public Map<String, Object> findById(Connection c, long id) throws SQLException {
    return Jdbc.one(c, "SELECT * FROM work_schedule WHERE schedule_id=? FOR UPDATE", id);
  }

  public List<Map<String, Object>> available() {
    return Database.tx(
        c ->
            ScheduleAvailability.filter(
                c,
                Jdbc.list(
                    c,
                    "SELECT w.*,u.full_name doctor_name,d.name department FROM work_schedule w JOIN"
                        + " users u ON u.user_id=w.doctor_id JOIN doctors doc ON"
                        + " doc.doctor_id=w.doctor_id JOIN departments d USING(department_id) WHERE"
                        + " w.status='AVAILABLE' AND w.work_date>=CURDATE() AND"
                        + " w.booked_count<w.max_slots AND u.status='ACTIVE' ORDER BY"
                        + " w.work_date,w.start_time")));
  }

  public int decrementBooked(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c, "UPDATE work_schedule SET booked_count=booked_count-1 WHERE schedule_id=?", values);
  }

  public int incrementBooked(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c, "UPDATE work_schedule SET booked_count=booked_count+1 WHERE schedule_id=?", values);
  }

  public int cancelShift(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE work_schedule SET status='CANCELLED',booked_count=0 WHERE schedule_id=?",
        values);
  }

  public int updateCapacity(Connection c, Object... values) throws SQLException {
    return Jdbc.update(c, "UPDATE work_schedule SET max_slots=? WHERE schedule_id=?", values);
  }

  public long insertScheduleRow(Connection c, Object... values) throws SQLException {
    return Jdbc.insert(
        c,
        "INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots)"
            + " VALUES(?,?,?,?,?,?)",
        values);
  }

  public Map<String, Object> findNamedShiftRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT schedule_id FROM work_schedule WHERE doctor_id=? AND work_date=? AND shift=?",
        values);
  }

  public Map<String, Object> findOverlappingRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c,
        "SELECT schedule_id FROM work_schedule WHERE doctor_id=? AND work_date=? AND"
            + " status<>'CANCELLED' AND start_time<? AND end_time>?",
        values);
  }

  public java.util.List<java.util.Map<String, Object>> findActiveDoctors(Object... values) {
    return Jdbc.read(
        "SELECT d.doctor_id,u.full_name FROM doctors d JOIN users u ON u.user_id=d.doctor_id WHERE"
            + " (?='ADMIN' OR d.doctor_id=?)",
        values);
  }

  public java.util.List<java.util.Map<String, Object>> findDoctorSchedules(Object... values) {
    return Jdbc.read(
        "SELECT w.*,u.full_name doctor_name FROM work_schedule w JOIN users u ON"
            + " u.user_id=w.doctor_id WHERE (?='ADMIN' OR w.doctor_id=?) ORDER BY work_date"
            + " DESC,start_time",
        values);
  }

  public vn.hospital.model.WorkSchedule findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM work_schedule WHERE schedule_id=?", id),
                vn.hospital.model.WorkSchedule.class));
  }
}
