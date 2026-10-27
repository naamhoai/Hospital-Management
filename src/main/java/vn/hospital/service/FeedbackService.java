package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.util.*;
import java.time.*;
public class FeedbackService {
    public void submitFeedback(long patient,long record,int rating,String comment) {
        Checks.require(rating>=1 && rating<=5,"Số sao phải từ 1 đến 5."); Checks.require(comment.length()<=2000,"Nhận xét tối đa 2000 ký tự.");
        Database.tx(c -> {
            var m=Jdbc.one(c,"SELECT * FROM medical_record WHERE record_id=? FOR UPDATE",record);
            Checks.require(m!=null && Jdbc.number(m,"patient_id")==patient && "CLOSED".equals(m.get("status")),"Chỉ được đánh giá bệnh án của bạn sau khi khám xong.");
            Checks.require(((java.sql.Timestamp)m.get("closed_at")).toLocalDateTime().plusDays(7).isAfter(LocalDateTime.now()),"Đã hết thời hạn đánh giá 7 ngày.");
            Checks.require(Jdbc.one(c,"SELECT feedback_id FROM feedback WHERE record_id=?",record)==null,"Bệnh án này đã được đánh giá.");
            Jdbc.one(c,"SELECT doctor_id FROM doctors WHERE doctor_id=? FOR UPDATE",m.get("doctor_id"));
            long id=Jdbc.insert(c,"INSERT INTO feedback(record_id,patient_id,doctor_id,rating,comment) VALUES(?,?,?,?,?)",record,patient,m.get("doctor_id"),rating,comment);
            Jdbc.update(c,"UPDATE doctors SET rating_average=(SELECT AVG(rating) FROM feedback WHERE doctor_id=?) WHERE doctor_id=?",m.get("doctor_id"),m.get("doctor_id"));
            AuditDAO.record(c,patient,"SUBMIT_FEEDBACK","feedback",id); return null;
        });
    }
}
