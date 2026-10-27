package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.integration.QueueContextPort;
import vn.hospital.model.*;
import vn.hospital.util.*;
import java.util.*;
public class EMRService {
    public Map<String,Object> getRecord(AuthUser doctor,long queue) {
        return Database.tx(c -> { var q=new QueueContextPort().getContext(c,queue); Checks.require(q!=null && Jdbc.number(q,"doctor_id")==doctor.getUserId(),"Bệnh nhân không được phân công cho bạn."); return editable(new MedicalRecordDAO().findByQueueId(c,queue),doctor,false); });
    }
    public Map<String,Object> recordClinicalDiagnosis(AuthUser doctor,long queue,DiagnosisRequest request) {
        String symptoms=Checks.text(request.symptoms(),5000,"Triệu chứng"), code=Checks.text(request.icdCode(),20,"Mã ICD"), diagnosis=Checks.text(request.diagnosis(),255,"Chẩn đoán"); Checks.require(request.notes().length()<=5000,"Ghi chú tối đa 5000 ký tự.");
        return Database.tx(c -> {
            var q=new QueueContextPort().getContext(c,queue); Checks.require(q!=null && Jdbc.number(q,"doctor_id")==doctor.getUserId() && "IN_CONSULTATION".equals(q.get("status")),"Lượt khám không thuộc bạn hoặc chưa được gọi.");
            var dao=new MedicalRecordDAO(); var m=editable(dao.findByQueueId(c,queue),doctor,true);
            Checks.require(Jdbc.one(c,"SELECT code FROM icd_catalog WHERE code=?",code)!=null,"Mã ICD chưa có trong danh mục đã cấu hình.");
            dao.save(c,Jdbc.number(m,"record_id"),symptoms,code,diagnosis,request.notes()); AuditDAO.record(c,doctor.getUserId(),"SAVE_DIAGNOSIS","medical_record",Jdbc.number(m,"record_id")); return dao.findById(c,Jdbc.number(m,"record_id"));
        });
    }
    public static Map<String,Object> editable(Map<String,Object> m,AuthUser doctor,boolean open) {
        Checks.require(m!=null && Jdbc.number(m,"doctor_id")==doctor.getUserId(),"Bạn không được truy cập bệnh án này."); if(open) Checks.require("OPEN".equals(m.get("status")),"Bệnh án đã kết thúc, không thể chỉnh sửa."); return m;
    }
    public void closeRecord(AuthUser doctor,long queue) {
        Database.tx(c -> {
            var m=editable(new MedicalRecordDAO().findByQueueId(c,queue),doctor,true);
            Checks.require(m.get("diagnosis")!=null && m.get("icd_code")!=null,"Phải lưu chẩn đoán trước khi kết thúc khám.");
            Checks.require(Jdbc.one(c,"SELECT order_id FROM clinical_order WHERE record_id=? AND status<>'COMPLETED'",m.get("record_id"))==null,"Vẫn còn chỉ định xét nghiệm chưa hoàn tất.");
            Jdbc.update(c,"UPDATE medical_record SET status='CLOSED',closed_at=NOW() WHERE record_id=?",m.get("record_id"));
            Jdbc.update(c,"UPDATE queue SET status='COMPLETED' WHERE queue_id=?",queue); Jdbc.update(c,"UPDATE appointment SET status='COMPLETED' WHERE appointment_id=?",m.get("appointment_id"));
            AppointmentService.notify(c,Jdbc.number(m,"patient_id"),"Hoàn tất khám bệnh","Bạn có thể đánh giá chất lượng khám tại mục Đánh giá trong 7 ngày."); AuditDAO.record(c,doctor.getUserId(),"CLOSE_RECORD","medical_record",Jdbc.number(m,"record_id")); return null;
        });
    }
}
