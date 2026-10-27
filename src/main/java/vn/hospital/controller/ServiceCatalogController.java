package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import vn.hospital.dao.*;
import vn.hospital.service.CatalogService;
import vn.hospital.util.Checks;
@WebServlet("/services")
public class ServiceCatalogController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException { r.setAttribute("services",Jdbc.read("SELECT s.*,d.name department FROM service s JOIN departments d USING(department_id) ORDER BY service_id")); r.setAttribute("departments",Jdbc.read("SELECT * FROM departments")); view(r,s,"services"); }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException { new CatalogService().save(user(r),p(r,"serviceId").isBlank()?0:id(r,"serviceId"),id(r,"departmentId"),p(r,"name"),p(r,"category"),Checks.money(p(r,"price")),p(r,"status")); redirect(r,s,"/services","Đã lưu dịch vụ. Giá mới áp dụng cho phát sinh tiếp theo."); }
}
