package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.model.*;
import vn.hospital.util.*;
import java.util.*;
public class PrescriptionService {
    public long issuePrescription(AuthUser doctor,long record,List<PrescriptionItem> items,String advice) {
        Checks.require(!items.isEmpty() && items.size()<=30,"Đơn thuốc cần từ 1 đến 30 dòng thuốc."); Checks.require(advice.length()<=255,"Lời dặn tối đa 255 ký tự.");
        var sorted=new ArrayList<>(items); sorted.sort(Comparator.comparingLong(PrescriptionItem::medicationId));
        Set<Long> ids=new HashSet<>(); for(var i:sorted) { Checks.require(ids.add(i.medicationId()),"Thuốc bị lặp trong đơn."); Checks.require(i.quantity()>0 && i.quantity()<=1000,"Số lượng thuốc từ 1 đến 1000."); Checks.text(i.dosage(),100,"Liều dùng"); }
        return Database.tx(c -> {
            var m=EMRService.editable(new MedicalRecordDAO().findById(c,record),doctor,true); Checks.require(m.get("diagnosis")!=null,"Phải lưu chẩn đoán trước khi kê đơn.");
            long rx=Jdbc.insert(c,"INSERT INTO prescription(record_id,advice) VALUES(?,?)",record,advice);
            for(var item:sorted) {
                var med=Jdbc.one(c,"SELECT * FROM medication WHERE medication_id=? FOR UPDATE",item.medicationId());
                Checks.require(med!=null && "ACTIVE".equals(med.get("status")) && Jdbc.number(med,"stock_quantity")>=item.quantity(),"Thuốc không khả dụng hoặc không đủ tồn kho: #"+item.medicationId());
                Jdbc.insert(c,"INSERT INTO prescription_detail(prescription_id,medication_id,quantity,dosage,unit_price) VALUES(?,?,?,?,?)",rx,item.medicationId(),item.quantity(),item.dosage(),med.get("unit_price"));
                Jdbc.update(c,"UPDATE medication SET stock_quantity=stock_quantity-? WHERE medication_id=?",item.quantity(),item.medicationId());
            } AuditDAO.record(c,doctor.getUserId(),"ISSUE_PRESCRIPTION","prescription",rx); return rx;
        });
    }
}
