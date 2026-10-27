package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import vn.hospital.dao.*;
import vn.hospital.service.*;
import vn.hospital.model.DiagnosisRequest;
@WebServlet("/emr")
public class EMRController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        r.setAttribute("records",Jdbc.read("SELECT m.*,u.full_name patient_name FROM medical_record m JOIN users u ON u.user_id=m.patient_id WHERE doctor_id=? ORDER BY record_id DESC",user(r).getUserId()));
        if(!p(r,"queueId").isBlank()) {
            var m=new EMRService().getRecord(user(r),id(r,"queueId")); r.setAttribute("record",m);
            r.setAttribute("history",Jdbc.read("SELECT record_id,diagnosis,icd_code,created_at FROM medical_record WHERE patient_id=? AND status='CLOSED' ORDER BY record_id DESC",m.get("patient_id")));
            r.setAttribute("orders",Jdbc.read("SELECT o.*,s.name,t.result_data,t.conclusion FROM clinical_order o JOIN service s USING(service_id) LEFT JOIN test_result t USING(order_id) WHERE record_id=?",m.get("record_id")));
            r.setAttribute("prescriptions",Jdbc.read("SELECT * FROM prescription WHERE record_id=? ORDER BY prescription_id DESC",m.get("record_id")));
        }
        r.setAttribute("icds",Jdbc.read("SELECT * FROM icd_catalog ORDER BY code")); r.setAttribute("services",Jdbc.read("SELECT * FROM service WHERE status='ACTIVE' AND category<>'CONSULTATION'")); r.setAttribute("medications",Jdbc.read("SELECT * FROM medication WHERE status='ACTIVE' AND stock_quantity>0")); view(r,s,"emr");
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        long queue=id(r,"queueId"); var service=new EMRService();
        if(p(r,"action").equals("close")) service.closeRecord(user(r),queue);
        else if(p(r,"action").equals("diagnosis")) service.recordClinicalDiagnosis(user(r),queue,new DiagnosisRequest(p(r,"symptoms"),p(r,"icdCode"),p(r,"diagnosis"),p(r,"notes")));
        else throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
        redirect(r,s,"/emr?queueId="+queue,"Đã cập nhật bệnh án.");
    }
}
