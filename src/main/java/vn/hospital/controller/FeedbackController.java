package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import vn.hospital.dao.*;
import vn.hospital.service.FeedbackService;
@WebServlet("/feedback")
public class FeedbackController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        r.setAttribute("records",Jdbc.read("SELECT m.record_id,m.diagnosis,m.closed_at,u.full_name doctor_name,f.rating FROM medical_record m JOIN users u ON u.user_id=m.doctor_id LEFT JOIN feedback f USING(record_id) WHERE m.patient_id=? AND m.status='CLOSED' ORDER BY m.record_id DESC",user(r).getUserId())); view(r,s,"feedback");
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException { new FeedbackService().submitFeedback(user(r).getUserId(),id(r,"recordId"),(int)id(r,"rating"),p(r,"comment")); redirect(r,s,"/feedback","Cảm ơn bạn đã đánh giá."); }
}
