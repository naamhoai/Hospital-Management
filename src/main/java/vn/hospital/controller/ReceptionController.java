package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import vn.hospital.dao.*;
import vn.hospital.service.QueueService;
@WebServlet("/reception")
public class ReceptionController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        String q="%"+p(r,"keyword")+"%";
        r.setAttribute("appointments",Jdbc.read("SELECT a.*,u.full_name patient_name,u.phone_number FROM appointment a JOIN users u ON u.user_id=a.patient_id WHERE appointment_date=CURDATE() AND (a.appointment_code LIKE ? OR u.phone_number LIKE ? OR u.full_name LIKE ?) ORDER BY appointment_time",q,q,q));
        r.setAttribute("patients",Jdbc.read("SELECT p.patient_id,u.full_name,u.email FROM patients p JOIN users u ON u.user_id=p.patient_id ORDER BY u.full_name")); r.setAttribute("schedules",new ScheduleDAO().available()); view(r,s,"reception");
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        var service=new QueueService(); long queue;
        if(p(r,"action").equals("walkin")) queue=service.registerWalkIn(user(r),id(r,"patientId"),id(r,"scheduleId"),p(r,"symptoms"),p(r,"priority"));
        else if(p(r,"action").equals("checkin")) queue=service.checkIn(user(r),id(r,"appointmentId"),p(r,"priority"));
        else throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
        redirect(r,s,"/reception","Đã cấp phiếu chờ #"+queue);
    }
}
