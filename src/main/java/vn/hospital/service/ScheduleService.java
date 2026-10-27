package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;
import java.time.*;
public class ScheduleService {
    public void createSchedule(AuthUser actor,long doctor,String date,String shift,String start,String end,int capacity) {
        Checks.require(actor.is("ADMIN") || actor.getUserId()==doctor,"Chỉ được tạo lịch của mình.");
        var day=Checks.date(date); Checks.require(!day.isBefore(LocalDate.now()),"Không tạo ca khám trong quá khứ.");
        LocalTime from,to; try { from=LocalTime.parse(start); to=LocalTime.parse(end); } catch(Exception e) { throw new BusinessException("Giờ ca khám không hợp lệ."); }
        Checks.require(from.isBefore(to) && capacity>0 && capacity<=Duration.between(from,to).toMinutes(),"Giờ hoặc sức chứa không hợp lệ.");
        String name=Checks.text(shift,20,"Ca khám");
        Database.tx(c -> {
            Checks.require(Jdbc.one(c,"SELECT doctor_id FROM doctors WHERE doctor_id=? FOR UPDATE",doctor)!=null,"Không tìm thấy bác sĩ.");
            Checks.require(Jdbc.one(c,"SELECT schedule_id FROM work_schedule WHERE doctor_id=? AND work_date=? AND status<>'CANCELLED' AND start_time<? AND end_time>?",doctor,day,to,from)==null,"Ca khám bị trùng giờ.");
            Checks.require(Jdbc.one(c,"SELECT schedule_id FROM work_schedule WHERE doctor_id=? AND work_date=? AND shift=?",doctor,day,name)==null,"Tên ca đã tồn tại trong ngày.");
            long id=Jdbc.insert(c,"INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(?,?,?,?,?,?)",doctor,day,name,from,to,capacity); AuditDAO.record(c,actor.getUserId(),"CREATE_SCHEDULE","work_schedule",id); return null;
        });
    }
    public void adjustCapacity(AuthUser actor,long schedule,int capacity) {
        Database.tx(c -> {
            var w=new ScheduleDAO().findById(c,schedule); own(actor,w);
            Checks.require(capacity>=Jdbc.number(w,"booked_count") && capacity>0 && capacity<=240,"Sức chứa không thể nhỏ hơn số lịch đã đặt; tối đa 240.");
            // Existing appointment times remain stable. Capacity changes are allowed before any booking.
            Checks.require(Jdbc.number(w,"booked_count")==0,"Ca đã có lịch đặt. Hãy tạo ca bổ sung để giữ nguyên giờ hẹn.");
            Checks.require(capacity<=Duration.between(((java.sql.Time)w.get("start_time")).toLocalTime(),((java.sql.Time)w.get("end_time")).toLocalTime()).toMinutes(),"Sức chứa vượt thời lượng ca.");
            Jdbc.update(c,"UPDATE work_schedule SET max_slots=? WHERE schedule_id=?",capacity,schedule); AuditDAO.record(c,actor.getUserId(),"ADJUST_CAPACITY","work_schedule",schedule); return null;
        });
    }
    public void requestLeave(AuthUser actor,long schedule) {
        Database.tx(c -> {
            var w=new ScheduleDAO().findById(c,schedule); own(actor,w);
            Checks.require(!((java.sql.Date)w.get("work_date")).toLocalDate().isBefore(LocalDate.now()),"Không hủy ca đã qua.");
            Checks.require(Jdbc.one(c,"SELECT appointment_id FROM appointment WHERE schedule_id=? AND status IN ('CHECKED_IN','COMPLETED')",schedule)==null,"Ca đã tiếp nhận bệnh nhân, không thể hủy.");
            for(var a:Jdbc.list(c,"SELECT patient_id,appointment_id FROM appointment WHERE schedule_id=? AND status='BOOKED'",schedule)) AppointmentService.notify(c,Jdbc.number(a,"patient_id"),"Bác sĩ nghỉ ca","Lịch #"+a.get("appointment_id")+" đã hủy. Vui lòng đặt lại.");
            Jdbc.update(c,"UPDATE appointment SET status='CANCELLED' WHERE schedule_id=? AND status='BOOKED'",schedule);
            Jdbc.update(c,"UPDATE work_schedule SET status='CANCELLED',booked_count=0 WHERE schedule_id=?",schedule); AuditDAO.record(c,actor.getUserId(),"LEAVE","work_schedule",schedule); return null;
        });
    }
    private void own(AuthUser actor,java.util.Map<String,Object> w) { Checks.require(w!=null && (actor.is("ADMIN") || Jdbc.number(w,"doctor_id")==actor.getUserId()) && !"CANCELLED".equals(w.get("status")),"Không được thay đổi ca này."); }
}
