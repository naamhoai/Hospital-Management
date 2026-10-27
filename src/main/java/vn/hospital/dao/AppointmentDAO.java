package vn.hospital.dao;
import java.sql.*;
import java.util.*;
public class AppointmentDAO {
    public Map<String,Object> findById(Connection c,long id) throws SQLException { return Jdbc.one(c,"SELECT * FROM appointment WHERE appointment_id=? FOR UPDATE",id); }
    public List<Map<String,Object>> findByPatient(long id) { return Jdbc.read("SELECT a.*,u.full_name doctor_name FROM appointment a JOIN users u ON a.doctor_id=u.user_id WHERE patient_id=? ORDER BY appointment_date DESC,appointment_time",id); }
}
