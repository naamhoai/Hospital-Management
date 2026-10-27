package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import vn.hospital.dao.*;
import vn.hospital.service.*;
@WebServlet("/billing")
public class BillingController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        r.setAttribute("invoices",new InvoiceDAO().list()); r.setAttribute("records",Jdbc.read("SELECT m.record_id,u.full_name patient_name,m.diagnosis FROM medical_record m JOIN users u ON u.user_id=m.patient_id ORDER BY record_id DESC"));
        if(!p(r,"id").isBlank()) { var rows=Jdbc.read("SELECT i.*,u.full_name patient_name FROM invoice i JOIN users u ON u.user_id=i.patient_id WHERE invoice_id=?",id(r,"id")); vn.hospital.util.Checks.require(!rows.isEmpty(),"Không tìm thấy hóa đơn."); r.setAttribute("invoice",rows.get(0)); r.setAttribute("lines",Jdbc.read("SELECT *,quantity*unit_price amount FROM invoice_line WHERE invoice_id=?",id(r,"id"))); }
        view(r,s,"billing");
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        switch(p(r,"action")) {
            case "create" -> { long invoice=new BillingService().createInvoice(user(r),id(r,"recordId")); redirect(r,s,"/billing?id="+invoice,"Đã lập hóa đơn."); }
            case "cash" -> { long invoice=id(r,"invoiceId"); new BillingService().processCashPayment(user(r),invoice); redirect(r,s,"/billing?id="+invoice,"Đã nhận thanh toán tiền mặt."); }
            case "vnpay" -> s.sendRedirect(new PaymentService().createLink(user(r),id(r,"invoiceId"),r.getRemoteAddr()));
            default -> throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
        }
    }
}
