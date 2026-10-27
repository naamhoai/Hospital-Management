package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.util.*;
import java.sql.*;
import java.time.*;
import java.util.*;
public class AppointmentService {
    private final ScheduleDAO schedules=new ScheduleDAO();
    private final AppointmentDAO appointments=new AppointmentDAO();
    public long createAppointment(long patient,long schedule,String symptoms) { return Database.tx(c -> book(c,patient,schedule,symptoms,true)); }
    public long book(Connection c,long patient,long schedule,String symptoms,boolean online) throws SQLException {
        var p=Jdbc.one(c,"SELECT * FROM patients WHERE patient_id=? FOR UPDATE",patient); Checks.require(p!=null,"Không tìm thấy bệnh nhân.");
        if(online) {
            Checks.require(p.get("booking_blocked_until")==null || ((java.sql.Date)p.get("booking_blocked_until")).toLocalDate().isBefore(LocalDate.now()),"Tạm khóa đặt lịch trực tuyến 30 ngày do hủy nhiều lần.");
            long count=Jdbc.number(Jdbc.one(c,"SELECT COUNT(*) n FROM appointment WHERE patient_id=? AND status IN ('BOOKED','CHECKED_IN')",patient),"n");
            Checks.require(count<2,"Bạn chỉ được có tối đa 2 lịch khám chưa hoàn tất.");
        }
        var w=schedules.findById(c,schedule); Checks.require(w!=null && "AVAILABLE".equals(w.get("status")),"Ca khám không khả dụng.");
        var slot=findFreeTime(c,w,0);
        var fee=Jdbc.one(c,"SELECT unit_price FROM service WHERE category='CONSULTATION' AND status='ACTIVE' ORDER BY service_id LIMIT 1"); Checks.require(fee!=null,"Chưa cấu hình phí khám.");
        long id=Jdbc.insert(c,"INSERT INTO appointment(appointment_code,patient_id,doctor_id,schedule_id,appointment_date,appointment_time,initial_symptoms,consultation_fee) VALUES(?,?,?,?,?,?,?,?)","AP-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(),patient,w.get("doctor_id"),schedule,w.get("work_date"),slot,Checks.text(symptoms,255,"Triệu chứng"),fee.get("unit_price"));
        Jdbc.update(c,"UPDATE work_schedule SET booked_count=booked_count+1 WHERE schedule_id=?",schedule);
        notify(c,patient,"Xác nhận đặt lịch","Mã lịch #"+id+", ngày "+w.get("work_date")+" lúc "+slot);
        AuditDAO.record(c,patient,"BOOK","appointment",id); return id;
    }
    private LocalTime findFreeTime(Connection c,Map<String,Object> w,long exclude) throws SQLException {
        Checks.require(Jdbc.number(w,"booked_count")<Jdbc.number(w,"max_slots"),"Ca khám đã đầy.");
        var day=((java.sql.Date)w.get("work_date")).toLocalDate(); var start=((Time)w.get("start_time")).toLocalTime(); var end=((Time)w.get("end_time")).toLocalTime();
        long minutes=Duration.between(start,end).toMinutes()/Jdbc.number(w,"max_slots"); Checks.require(minutes>0,"Sức chứa ca khám quá lớn.");
        var booked=Jdbc.list(c,"SELECT appointment_time FROM appointment WHERE schedule_id=? AND appointment_id<>? AND status IN ('BOOKED','CHECKED_IN','COMPLETED') FOR UPDATE",w.get("schedule_id"),exclude);
        Set<LocalTime> taken=new HashSet<>(); for(var b:booked) taken.add(((Time)b.get("appointment_time")).toLocalTime());
        for(int i=0;i<Jdbc.number(w,"max_slots");i++) { LocalTime time=start.plusMinutes(i*minutes); if(!taken.contains(time) && LocalDateTime.of(day,time).isAfter(LocalDateTime.now())) return time; }
        throw new BusinessException("Không còn giờ khám phù hợp trong ca này.");
    }
    public void rescheduleAppointment(long patient,long appointment,long target) {
        Database.tx(c -> {
            Jdbc.one(c,"SELECT patient_id FROM patients WHERE patient_id=? FOR UPDATE",patient);
            var a=appointments.findById(c,appointment); own(a,patient);
            Checks.require(Jdbc.number(a,"schedule_id")!=target,"Hãy chọn ca khám khác.");
            Checks.require(Jdbc.number(a,"reschedule_count")<2,"Chỉ được đổi lịch tối đa 2 lần.");
            var old=schedules.findById(c,Jdbc.number(a,"schedule_id"));
            Checks.require(LocalDateTime.of(((java.sql.Date)old.get("work_date")).toLocalDate(),((Time)old.get("start_time")).toLocalTime()).isAfter(LocalDateTime.now().plusHours(12)),"Phải đổi lịch trước giờ bắt đầu ca ít nhất 12 tiếng.");
            var w=schedules.findById(c,target); Checks.require(w!=null && "AVAILABLE".equals(w.get("status")),"Ca khám không khả dụng.");
            var time=findFreeTime(c,w,appointment);
            Jdbc.update(c,"UPDATE appointment SET schedule_id=?,doctor_id=?,appointment_date=?,appointment_time=?,reschedule_count=reschedule_count+1 WHERE appointment_id=?",target,w.get("doctor_id"),w.get("work_date"),time,appointment);
            Jdbc.update(c,"UPDATE work_schedule SET booked_count=booked_count-1 WHERE schedule_id=?",a.get("schedule_id"));
            Jdbc.update(c,"UPDATE work_schedule SET booked_count=booked_count+1 WHERE schedule_id=?",target);
            notify(c,patient,"Đổi lịch khám","Lịch #"+appointment+" chuyển sang "+w.get("work_date")+" "+time); AuditDAO.record(c,patient,"RESCHEDULE","appointment",appointment); return null;
        });
    }
    public void cancelAppointment(long patient,long appointment) {
        Database.tx(c -> {
            Jdbc.one(c,"SELECT patient_id FROM patients WHERE patient_id=? FOR UPDATE",patient);
            var a=appointments.findById(c,appointment); own(a,patient);
            Jdbc.update(c,"UPDATE appointment SET status='CANCELLED',cancelled_at=NOW() WHERE appointment_id=?",appointment);
            Jdbc.update(c,"UPDATE work_schedule SET booked_count=booked_count-1 WHERE schedule_id=?",a.get("schedule_id"));
            long cancellations=Jdbc.number(Jdbc.one(c,"SELECT COUNT(*) n FROM appointment WHERE patient_id=? AND cancelled_at>=DATE_FORMAT(CURDATE(),'%Y-%m-01')",patient),"n");
            if(cancellations>3) Jdbc.update(c,"UPDATE patients SET booking_blocked_until=DATE_ADD(CURDATE(),INTERVAL 30 DAY) WHERE patient_id=?",patient);
            notify(c,patient,"Hủy lịch khám","Đã hủy lịch #"+appointment); AuditDAO.record(c,patient,"CANCEL","appointment",appointment); return null;
        });
    }
    private void own(Map<String,Object> a,long patient) { Checks.require(a!=null && Jdbc.number(a,"patient_id")==patient && "BOOKED".equals(a.get("status")),"Không thể thay đổi lịch khám này."); }
    public static void notify(Connection c,long patient,String subject,String body) throws SQLException { Jdbc.update(c,"INSERT INTO notification_outbox(recipient,subject,body) SELECT email,?,? FROM users WHERE user_id=?",subject,body,patient); }
}
