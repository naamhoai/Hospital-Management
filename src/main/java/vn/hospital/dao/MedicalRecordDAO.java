package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.model.MedicalRecord;
import vn.hospital.util.Database;

public class MedicalRecordDAO {
  public MedicalRecord findById(Integer id) {
    return Database.tx(c -> findById(c, id));
  }

  public MedicalRecord findById(Connection c, long id) throws SQLException {
    return BeanMapper.map(findByIdRow(c, id), MedicalRecord.class);
  }

  public Map<String, Object> findByIdRow(Connection c, long id) throws SQLException {
    return Jdbc.one(c, "SELECT * FROM medical_record WHERE record_id=? FOR UPDATE", id);
  }

  public MedicalRecord findByQueueId(Integer queueId) {
    return Database.tx(c -> findByQueueId(c, queueId));
  }

  public MedicalRecord findByQueueId(Connection c, long queueId) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(
            c,
            "SELECT m.* FROM medical_record m JOIN queue q ON q.appointment_id=m.appointment_id"
                + " WHERE q.queue_id=? FOR UPDATE",
            queueId),
        MedicalRecord.class);
  }

  public List<MedicalRecord> findByPatientId(Integer patientId) {
    return Database.tx(c -> findByPatientId(c, patientId));
  }

  public List<MedicalRecord> findByPatientId(Connection c, long patientId) throws SQLException {
    return BeanMapper.list(
        Jdbc.list(
            c,
            "SELECT * FROM medical_record WHERE patient_id=? ORDER BY record_id DESC",
            patientId),
        MedicalRecord.class);
  }

  public MedicalRecord save(MedicalRecord record) {
    return Database.tx(c -> save(c, record));
  }

  public MedicalRecord save(Connection c, MedicalRecord record) throws SQLException {
    Jdbc.update(
        c,
        "UPDATE medical_record SET symptoms=?,icd_code=?,diagnosis=?,notes=? WHERE record_id=?",
        record.getSymptoms(),
        record.getIcdCode(),
        record.getDiagnosis(),
        record.getNotes(),
        record.getRecordId());
    return findById(c, record.getRecordId());
  }

  public boolean assignedPatient(Connection c, long doctor, long patient) throws SQLException {
    return Jdbc.one(
            c,
            "SELECT record_id FROM medical_record WHERE doctor_id=? AND patient_id=? LIMIT 1",
            doctor,
            patient)
        != null;
  }

  public List<Map<String, Object>> findConsultations(Connection c, long doctor, String keyword)
      throws SQLException {
    return Jdbc.list(
        c,
        "SELECT"
            + " m.record_id,q.queue_id,m.status,m.diagnosis,q.queue_number,q.room_assigned,m.created_at,u.full_name"
            + " patient_name,u.phone_number,p.date_of_birth FROM medical_record m JOIN queue q ON"
            + " q.appointment_id=m.appointment_id JOIN users u ON u.user_id=m.patient_id JOIN"
            + " patients p ON p.patient_id=m.patient_id WHERE m.doctor_id=? AND (u.full_name LIKE ?"
            + " OR u.phone_number LIKE ?) ORDER BY CASE m.status WHEN 'OPEN' THEN 0 ELSE 1"
            + " END,m.record_id DESC LIMIT 100",
        doctor,
        "%" + keyword + "%",
        "%" + keyword + "%");
  }

  public Map<String, Object> patientProfile(Connection c, long patient) throws SQLException {
    return Jdbc.one(
        c,
        "SELECT u.full_name,u.phone_number,u.gender,p.* FROM patients p JOIN users u ON"
            + " u.user_id=p.patient_id WHERE p.patient_id=?",
        patient);
  }

  public List<Map<String, Object>> findClosedHistory(Connection c, long patient)
      throws SQLException {
    return Jdbc.list(
        c,
        "SELECT m.record_id,m.diagnosis,m.icd_code,m.symptoms,m.notes,m.closed_at,u.full_name"
            + " doctor_name FROM medical_record m JOIN users u ON u.user_id=m.doctor_id WHERE"
            + " patient_id=? AND m.status='CLOSED' ORDER BY record_id DESC",
        patient);
  }

  public int close(Connection c, long id) throws SQLException {
    return Jdbc.update(
        c,
        "UPDATE medical_record SET status='CLOSED',closed_at=NOW() WHERE record_id=? AND"
            + " status='OPEN'",
        id);
  }

  public void completeEncounter(Connection c, MedicalRecord record) throws SQLException {
    Jdbc.update(
        c, "UPDATE queue SET status='COMPLETED' WHERE appointment_id=?", record.getAppointmentId());
    Jdbc.update(
        c,
        "UPDATE appointment SET status='COMPLETED' WHERE appointment_id=?",
        record.getAppointmentId());
  }

  public long createDraftForQueue(Connection c, Object... values) throws SQLException {
    return Jdbc.insert(
        c,
        "INSERT INTO medical_record(appointment_id,patient_id,doctor_id,diagnosis) SELECT"
            + " appointment_id,patient_id,doctor_id,'' FROM queue WHERE queue_id=?",
        values[0]);
  }

  public List<Map<String, Object>> findPatientRecordSummaries(Connection c, long patient)
      throws SQLException {
    return Jdbc.list(
        c,
        "SELECT m.record_id,m.diagnosis,m.icd_code,m.status,m.created_at,u.full_name doctor_name"
            + " FROM medical_record m JOIN users u ON u.user_id=m.doctor_id WHERE m.patient_id=?"
            + " ORDER BY m.record_id DESC",
        patient);
  }

  public String doctorName(Connection c, long id) throws SQLException {
    return (String) Jdbc.one(c, "SELECT full_name FROM users WHERE user_id=?", id).get("full_name");
  }

  public MedicalRecord findOwnedPatientRecord(Connection c, long id, long patient)
      throws SQLException {
    return BeanMapper.map(
        Jdbc.one(c, "SELECT * FROM medical_record WHERE record_id=? AND patient_id=?", id, patient),
        MedicalRecord.class);
  }

  public MedicalRecord findByIdRead(Connection c, long id) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(c, "SELECT * FROM medical_record WHERE record_id=?", id), MedicalRecord.class);
  }

  public MedicalRecord findByQueueIdRead(Connection c, long id) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(
            c,
            "SELECT m.* FROM medical_record m JOIN queue q ON q.appointment_id=m.appointment_id"
                + " WHERE q.queue_id=?",
            id),
        MedicalRecord.class);
  }
}
