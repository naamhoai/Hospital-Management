package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.*;
import vn.hospital.service.PaymentService;
import vn.hospital.util.BusinessException;
@WebServlet(urlPatterns={"/payment/return","/payment/ipn"})
public class PaymentCallbackController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        Map<String,String> params=new HashMap<>(); r.getParameterMap().forEach((k,v) -> { if(v.length==1) params.put(k,v[0]); });
        if(r.getServletPath().endsWith("/ipn")) {
            s.setContentType("application/json"); try { new PaymentService().handleCallback(params); s.getWriter().write("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}"); }
            catch(BusinessException e) { s.getWriter().write("{\"RspCode\":\"99\",\"Message\":\"Validation failed\"}"); } return;
        }
        r.setAttribute("message",new PaymentService().handleCallback(params)); view(r,s,"payment-result");
    }
}
