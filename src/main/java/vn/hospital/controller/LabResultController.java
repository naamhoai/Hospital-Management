package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import vn.hospital.dao.*;
import vn.hospital.service.ClinicalService;
@WebServlet("/lab")
public class LabResultController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        r.setAttribute("orders",Jdbc.read("SELECT o.*,s.name,u.full_name patient_name,t.result_data,t.conclusion FROM clinical_order o JOIN service s USING(service_id) JOIN medical_record m USING(record_id) JOIN users u ON u.user_id=m.patient_id LEFT JOIN test_result t USING(order_id) ORDER BY order_id DESC")); view(r,s,"lab");
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException { new ClinicalService().recordTestResult(user(r),id(r,"orderId"),p(r,"resultData"),p(r,"conclusion"),p(r,"fileUrl")); redirect(r,s,"/lab","Đã duyệt kết quả xét nghiệm."); }
}
