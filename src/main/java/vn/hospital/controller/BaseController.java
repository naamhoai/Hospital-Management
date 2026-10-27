package vn.hospital.controller;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;
public abstract class BaseController extends HttpServlet {
    protected AuthUser user(HttpServletRequest r) { return (AuthUser)r.getSession().getAttribute("user"); }
    protected String p(HttpServletRequest r,String key) { String s=r.getParameter(key); return s==null?"":s.trim(); }
    protected long id(HttpServletRequest r,String key) { return Checks.id(p(r,key)); }
    protected void view(HttpServletRequest r,HttpServletResponse s,String name) throws ServletException,IOException { r.getRequestDispatcher("/WEB-INF/views/"+name+".jsp").forward(r,s); }
    protected void redirect(HttpServletRequest r,HttpServletResponse s,String path,String message) throws IOException { r.getSession().setAttribute("flash",message); s.sendRedirect(r.getContextPath()+path); }
    @Override protected void service(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        try { super.service(r,s); }
        catch(BusinessException e) { s.setStatus(422); r.setAttribute("error",e.getMessage()); view(r,s,"error"); }
        catch(RuntimeException e) { getServletContext().log("Request failed: "+r.getServletPath(),e); s.setStatus(500); r.setAttribute("error","Không xử lý được yêu cầu. Kiểm tra cấu hình database hoặc xem log Tomcat."); view(r,s,"error"); }
    }
}
