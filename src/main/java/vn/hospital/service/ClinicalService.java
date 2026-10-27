package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;
public class ClinicalService {
    public void createClinicalOrders(AuthUser doctor,long record,long[] services,String instructions) {
        Checks.require(services.length>0 && services.length<=20,"Chọn từ 1 đến 20 dịch vụ."); Checks.require(instructions.length()<=255,"Chỉ dẫn tối đa 255 ký tự.");
        Database.tx(c -> {
            var m=EMRService.editable(new MedicalRecordDAO().findById(c,record),doctor,true); Checks.require(m.get("diagnosis")!=null,"Cần chẩn đoán sơ bộ trước khi chỉ định xét nghiệm.");
            for(long id:services) {
                var service=Jdbc.one(c,"SELECT * FROM service WHERE service_id=? AND status='ACTIVE' AND category<>'CONSULTATION' FOR SHARE",id); Checks.require(service!=null,"Dịch vụ không khả dụng.");
                Checks.require(Jdbc.one(c,"SELECT order_id FROM clinical_order WHERE record_id=? AND service_id=?",record,id)==null,"Dịch vụ đã được chỉ định.");
                long order=Jdbc.insert(c,"INSERT INTO clinical_order(record_id,service_id,instructions,unit_price) VALUES(?,?,?,?)",record,id,instructions,service.get("unit_price")); AuditDAO.record(c,doctor.getUserId(),"ORDER_TEST","clinical_order",order);
            } return null;
        });
    }
    public void recordTestResult(AuthUser technician,long order,String data,String conclusion,String url) {
        String result=Checks.text(data,10000,"Kết quả"); Checks.require(conclusion.length()<=255 && url.length()<=255,"Kết luận hoặc URL quá dài.");
        Checks.require(url.isBlank() || url.startsWith("https://"),"Tệp đính kèm phải là URL HTTPS.");
        Database.tx(c -> {
            var o=Jdbc.one(c,"SELECT * FROM clinical_order WHERE order_id=? FOR UPDATE",order);
            Checks.require(o!=null && "PAID".equals(o.get("payment_status")) && "PENDING".equals(o.get("status")),"Chỉ được nhập kết quả cho chỉ định đã thanh toán, chưa hoàn tất.");
            Checks.require(Jdbc.one(c,"SELECT result_id FROM test_result WHERE order_id=?",order)==null,"Kết quả đã duyệt, không được sửa.");
            long id=Jdbc.insert(c,"INSERT INTO test_result(order_id,technician_id,result_data,conclusion,file_url) VALUES(?,?,?,?,?)",order,technician.getUserId(),result,conclusion,url);
            Jdbc.update(c,"UPDATE clinical_order SET status='COMPLETED' WHERE order_id=?",order); AuditDAO.record(c,technician.getUserId(),"APPROVE_RESULT","test_result",id); return null;
        });
    }
}
