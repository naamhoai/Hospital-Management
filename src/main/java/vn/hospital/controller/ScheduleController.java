package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import vn.hospital.dao.*;
import vn.hospital.service.ScheduleService;
@WebServlet("/schedules")
public class ScheduleController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        r.setAttribute("schedules",Jdbc.read("SELECT w.*,u.full_name doctor_name FROM work_schedule w JOIN users u ON u.user_id=w.doctor_id WHERE (?='ADMIN' OR w.doctor_id=?) ORDER BY work_date DESC,start_time",user(r).getRole(),user(r).getUserId()));
        r.setAttribute("doctors",Jdbc.read("SELECT d.doctor_id,u.full_name FROM doctors d JOIN users u ON u.user_id=d.doctor_id WHERE (?='ADMIN' OR d.doctor_id=?)",user(r).getRole(),user(r).getUserId())); view(r,s,"schedules");
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        var service=new ScheduleService(); switch(p(r,"action")) {
            case "create" -> service.createSchedule(user(r),id(r,"doctorId"),p(r,"date"),p(r,"shift"),p(r,"start"),p(r,"end"),(int)id(r,"capacity"));
            case "capacity" -> service.adjustCapacity(user(r),id(r,"scheduleId"),(int)id(r,"capacity"));
            case "leave" -> service.requestLeave(user(r),id(r,"scheduleId"));
            default -> throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
        } redirect(r,s,"/schedules","Đã cập nhật ca khám.");
    }
}
