package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import vn.hospital.dao.*;
import vn.hospital.service.QueueService;
@WebServlet("/queue")
public class QueueController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        r.setAttribute("queue",Jdbc.read("SELECT q.*,u.full_name patient_name,d.full_name doctor_name FROM queue q JOIN users u ON u.user_id=q.patient_id JOIN users d ON d.user_id=q.doctor_id WHERE work_date=CURDATE() AND (?<>'DOCTOR' OR q.doctor_id=?) ORDER BY room_assigned,CASE priority_type WHEN 'EMERGENCY' THEN 0 WHEN 'ELDERLY' THEN 1 ELSE 2 END,queue_number",user(r).getRole(),user(r).getUserId()));
        r.setAttribute("rooms",Jdbc.read("SELECT * FROM rooms WHERE status='ACTIVE' AND room_type='CONSULTATION'")); view(r,s,"queue");
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        if(p(r,"action").equals("next")) { vn.hospital.util.Checks.require(user(r).is("DOCTOR"),"Chỉ bác sĩ được gọi bệnh nhân."); Long id=new QueueService().callNext(user(r)); if(id!=null) { s.sendRedirect(r.getContextPath()+"/emr?queueId="+id); return; } redirect(r,s,"/queue","Không có bệnh nhân chờ."); }
        else if(p(r,"action").equals("route")) { vn.hospital.util.Checks.require(user(r).is("ADMIN","RECEPTIONIST"),"Chỉ lễ tân được điều phối."); new QueueService().routePatient(user(r),id(r,"queueId"),p(r,"room")); redirect(r,s,"/queue","Đã chuyển phòng khám."); }
        else throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
    }
}
