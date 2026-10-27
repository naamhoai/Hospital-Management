package vn.hospital.dao;
import java.util.*;
public class SearchDAO {
    public List<Map<String,Object>> doctors(String keyword,String date) {
        String q="%"+keyword+"%";
        Object day=date.isBlank()?null:java.time.LocalDate.parse(date);
        return Database.tx(c -> ScheduleAvailability.filter(c,Jdbc.list(c,"SELECT u.full_name,d.academic_title,dep.name department,d.room_number,d.rating_average,w.schedule_id,w.max_slots,w.work_date,w.shift,w.start_time,w.end_time FROM doctors d JOIN users u ON u.user_id=d.doctor_id JOIN departments dep USING(department_id) JOIN work_schedule w ON w.doctor_id=d.doctor_id WHERE u.status='ACTIVE' AND dep.status='ACTIVE' AND w.status='AVAILABLE' AND w.work_date>=CURDATE() AND w.max_slots>w.booked_count AND (u.full_name LIKE ? OR dep.name LIKE ?) AND (? IS NULL OR w.work_date=?) ORDER BY w.work_date,w.start_time",q,q,day,day)));
    }
    public List<Map<String,Object>> services(String keyword) { return Jdbc.read("SELECT s.*,d.name department FROM service s JOIN departments d USING(department_id) WHERE s.status='ACTIVE' AND s.name LIKE ? ORDER BY s.name","%"+keyword+"%"); }
}
