package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.service.*;
import vn.hospital.model.PrescriptionItem;
import vn.hospital.util.*;
@WebServlet("/prescriptions")
public class PrescriptionController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        long id=id(r,"id"); var rows=Jdbc.read("SELECT p.*,m.patient_id,m.doctor_id,m.diagnosis,u.full_name patient_name,d.full_name doctor_name FROM prescription p JOIN medical_record m USING(record_id) JOIN users u ON u.user_id=m.patient_id JOIN users d ON d.user_id=m.doctor_id WHERE prescription_id=? AND m.doctor_id=?",id,user(r).getUserId());
        Checks.require(!rows.isEmpty(),"Không tìm thấy đơn thuốc của bạn."); r.setAttribute("prescription",rows.get(0)); r.setAttribute("items",Jdbc.read("SELECT pd.*,m.name,m.unit FROM prescription_detail pd JOIN medication m USING(medication_id) WHERE prescription_id=?",id)); view(r,s,"prescription");
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        String[] meds=r.getParameterValues("medicationId"), quantities=r.getParameterValues("quantity"), dosages=r.getParameterValues("dosage");
        Checks.require(meds!=null && quantities!=null && dosages!=null && meds.length==quantities.length && meds.length==dosages.length,"Dòng thuốc không hợp lệ.");
        var items=new ArrayList<PrescriptionItem>(); for(int i=0;i<meds.length;i++) if(!meds[i].isBlank()) items.add(new PrescriptionItem(Checks.id(meds[i]),(int)Checks.id(quantities[i]),dosages[i]));
        long rx=new PrescriptionService().issuePrescription(user(r),id(r,"recordId"),items,p(r,"advice")); redirect(r,s,"/prescriptions?id="+rx,"Đã tạo đơn thuốc.");
    }
}
