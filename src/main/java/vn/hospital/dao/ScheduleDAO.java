package vn.hospital.dao;
import java.sql.*;
import java.util.*;
public class ScheduleDAO {
    public Map<String,Object> findById(Connection c,long id) throws SQLException { return Jdbc.one(c,"SELECT * FROM work_schedule WHERE schedule_id=? FOR UPDATE",id); }
    public List<Map<String,Object>> available() { return Database.tx(c -> ScheduleAvailability.filter(c,Jdbc.list(c,"SELECT w.*,u.full_name doctor_name,d.name department FROM work_schedule w JOIN users u ON u.user_id=w.doctor_id JOIN doctors doc ON doc.doctor_id=w.doctor_id JOIN departments d USING(department_id) WHERE w.status='AVAILABLE' AND w.work_date>=CURDATE() AND w.booked_count<w.max_slots AND u.status='ACTIVE' ORDER BY w.work_date,w.start_time"))); }
}
