package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;
import java.sql.*;
import java.time.*;
import java.util.*;
public class QueueService {
    public long checkIn(AuthUser actor,long appointment,String priority) { return Database.tx(c -> issueTicket(c,actor,appointment,priority)); }
    public long registerWalkIn(AuthUser actor,long patient,long schedule,String symptoms,String priority) {
        return Database.tx(c -> { long appt=new AppointmentService().book(c,patient,schedule,symptoms,false); return issueTicket(c,actor,appt,priority); });
    }
    private long issueTicket(Connection c,AuthUser actor,long appointment,String priority) throws SQLException {
        Checks.require(Set.of("STANDARD","ELDERLY","EMERGENCY").contains(priority),"Mức ưu tiên không hợp lệ.");
        var a=new AppointmentDAO().findById(c,appointment); Checks.require(a!=null && "BOOKED".equals(a.get("status")),"Lịch không tồn tại hoặc đã tiếp nhận.");
        Checks.require(((java.sql.Date)a.get("appointment_date")).toLocalDate().equals(LocalDate.now()),"Chỉ tiếp nhận lịch khám trong hôm nay.");
        var doc=Jdbc.one(c,"SELECT d.room_number FROM doctors d JOIN rooms r ON d.room_number=r.room_number WHERE doctor_id=? AND r.status='ACTIVE'",a.get("doctor_id")); Checks.require(doc!=null,"Phòng khám không khả dụng.");
        String room=(String)doc.get("room_number");
        Jdbc.update(c,"INSERT INTO queue_counter(work_date,room_number,last_number) VALUES(CURDATE(),?,0) ON DUPLICATE KEY UPDATE last_number=last_number",room);
        var counter=Jdbc.one(c,"SELECT last_number FROM queue_counter WHERE work_date=CURDATE() AND room_number=? FOR UPDATE",room); long n=Jdbc.number(counter,"last_number")+1;
        Jdbc.update(c,"UPDATE queue_counter SET last_number=? WHERE work_date=CURDATE() AND room_number=?",n,room);
        long id=Jdbc.insert(c,"INSERT INTO queue(appointment_id,patient_id,doctor_id,work_date,queue_number,room_assigned,priority_type) VALUES(?,?,?,CURDATE(),?,?,?)",appointment,a.get("patient_id"),a.get("doctor_id"),n,room,priority);
        Jdbc.update(c,"UPDATE appointment SET status='CHECKED_IN' WHERE appointment_id=?",appointment); AuditDAO.record(c,actor.getUserId(),"CHECK_IN","queue",id); return id;
    }
    public Long callNext(AuthUser doctor) {
        return Database.tx(c -> {
            Jdbc.one(c,"SELECT doctor_id FROM doctors WHERE doctor_id=? FOR UPDATE",doctor.getUserId());
            Checks.require(Jdbc.one(c,"SELECT queue_id FROM queue WHERE doctor_id=? AND status='IN_CONSULTATION'",doctor.getUserId())==null,"Hãy hoàn tất bệnh nhân đang khám trước.");
            var q=Jdbc.one(c,"SELECT q.* FROM queue q JOIN appointment a USING(appointment_id) WHERE q.doctor_id=? AND q.work_date=CURDATE() AND q.status='WAITING' ORDER BY CASE q.priority_type WHEN 'EMERGENCY' THEN 0 WHEN 'ELDERLY' THEN 1 ELSE 2 END,a.appointment_time,q.queue_number LIMIT 1 FOR UPDATE",doctor.getUserId());
            if(q==null) return null;
            Jdbc.update(c,"UPDATE queue SET status='IN_CONSULTATION' WHERE queue_id=?",q.get("queue_id"));
            Jdbc.insert(c,"INSERT INTO medical_record(queue_id,appointment_id,patient_id,doctor_id) VALUES(?,?,?,?)",q.get("queue_id"),q.get("appointment_id"),q.get("patient_id"),doctor.getUserId());
            AuditDAO.record(c,doctor.getUserId(),"CALL_NEXT","queue",Jdbc.number(q,"queue_id")); return Jdbc.number(q,"queue_id");
        });
    }
    public void routePatient(AuthUser actor,long queue,String room) {
        Database.tx(c -> {
            var q=Jdbc.one(c,"SELECT q.*,d.department_id FROM queue q JOIN doctors d ON q.doctor_id=d.doctor_id WHERE queue_id=? FOR UPDATE",queue);
            Checks.require(q!=null && "WAITING".equals(q.get("status")),"Chỉ được chuyển bệnh nhân đang chờ.");
            Checks.require(Jdbc.one(c,"SELECT room_id FROM rooms WHERE room_number=? AND department_id=? AND room_type='CONSULTATION' AND status='ACTIVE'",room,q.get("department_id"))!=null,"Phòng phải thuộc cùng chuyên khoa và đang hoạt động.");
            Jdbc.update(c,"INSERT INTO queue_counter(work_date,room_number,last_number) VALUES(CURDATE(),?,0) ON DUPLICATE KEY UPDATE last_number=last_number",room);
            var n=Jdbc.one(c,"SELECT last_number FROM queue_counter WHERE work_date=CURDATE() AND room_number=? FOR UPDATE",room);
            long number=Jdbc.number(n,"last_number")+1; Jdbc.update(c,"UPDATE queue_counter SET last_number=? WHERE work_date=CURDATE() AND room_number=?",number,room);
            Jdbc.update(c,"UPDATE queue SET room_assigned=?,queue_number=? WHERE queue_id=?",room,number,queue); AuditDAO.record(c,actor.getUserId(),"ROUTE","queue",queue); return null;
        });
    }
}
