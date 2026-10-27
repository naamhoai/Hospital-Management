package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public class QueueDAO {
  public int routeRow(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c, "UPDATE queue SET room_assigned=?,queue_number=? WHERE queue_id=?", values);
  }

  public int updateCounter(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE queue_counter SET last_number=? WHERE work_date=CURDATE() AND room_number=?",
        values);
  }

  public Map<String, Object> lockCounterRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(
        c,
        "SELECT last_number FROM queue_counter WHERE work_date=CURDATE() AND room_number=? FOR"
            + " UPDATE",
        values);
  }

  public int ensureCounter(Connection c, Object... values) throws SQLException {
    return Jdbc.update(
        c,
        "INSERT INTO queue_counter(work_date,room_number,last_number) VALUES(CURDATE(),?,0) ON"
            + " DUPLICATE KEY UPDATE last_number=last_number",
        values);
  }

  public Map<String, Object> lockRoutingRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(
        c,
        "SELECT q.*,d.department_id FROM queue q JOIN doctors d ON q.doctor_id=d.doctor_id WHERE"
            + " queue_id=? FOR UPDATE",
        values);
  }

  public int markInConsultation(Connection c, Object... values) throws SQLException {
    return Jdbc.update(c, "UPDATE queue SET status='IN_CONSULTATION' WHERE queue_id=?", values);
  }

  public Map<String, Object> lockNextRows(Connection c, Object... values) throws SQLException {
    return Jdbc.one(
        c,
        "SELECT q.* FROM queue q JOIN appointment a USING(appointment_id) WHERE q.doctor_id=? AND"
            + " q.work_date=CURDATE() AND q.status='WAITING' ORDER BY CASE q.priority_type WHEN"
            + " 'EMERGENCY' THEN 0 WHEN 'ELDERLY' THEN 1 ELSE 2"
            + " END,a.appointment_time,q.queue_number LIMIT 1 FOR UPDATE",
        values);
  }

  public Map<String, Object> findCurrentConsultationRows(Connection c, Object... values)
      throws SQLException {
    return Jdbc.one(
        c, "SELECT queue_id FROM queue WHERE doctor_id=? AND status='IN_CONSULTATION'", values);
  }

  public long insertQueueRow(Connection c, Object... values) throws SQLException {
    return Jdbc.insert(
        c,
        "INSERT INTO"
            + " queue(appointment_id,patient_id,doctor_id,work_date,queue_number,room_assigned,priority_type)"
            + " VALUES(?,?,?,CURDATE(),?,?,?)",
        values);
  }

  public java.util.List<java.util.Map<String, Object>> findActiveRooms(Object... values) {
    return Jdbc.read(
        "SELECT * FROM rooms WHERE status='ACTIVE' AND room_type='CONSULTATION'", values);
  }

  public java.util.List<java.util.Map<String, Object>> findQueueForActor(Object... values) {
    return Jdbc.read(
        "SELECT q.*,u.full_name patient_name,d.full_name doctor_name FROM queue q JOIN users u ON"
            + " u.user_id=q.patient_id JOIN users d ON d.user_id=q.doctor_id WHERE"
            + " work_date=CURDATE() AND (?<>'DOCTOR' OR q.doctor_id=?) ORDER BY room_assigned,CASE"
            + " priority_type WHEN 'EMERGENCY' THEN 0 WHEN 'ELDERLY' THEN 1 ELSE 2"
            + " END,queue_number",
        values);
  }

  public vn.hospital.model.QueueContext findContext(Connection c, long queueId)
      throws SQLException {
    return BeanMapper.map(
        Jdbc.one(
            c,
            "SELECT queue_id,appointment_id,patient_id,doctor_id,status,room_assigned FROM queue"
                + " WHERE queue_id=?",
            queueId),
        vn.hospital.model.QueueContext.class);
  }

  public vn.hospital.model.Queue findById(Integer id) {
    return Database.read(
        c ->
            BeanMapper.map(
                Jdbc.one(c, "SELECT * FROM queue WHERE queue_id=?", id),
                vn.hospital.model.Queue.class));
  }
}
