package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;

@WebServlet("/billing")
public class BillingController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    r.setAttribute("invoices", new BillingService().getInvoices());
    r.setAttribute("records", new BillingService().findBillableRecords());
    if (!p(r, "id").isBlank()) {
      var rows = new BillingService().findInvoiceDetails(id(r, "id"));
      vn.hospital.util.Checks.require(!rows.isEmpty(), "Không tìm thấy hóa đơn.");
      r.setAttribute("invoice", rows.get(0));
      r.setAttribute("lines", new BillingService().findInvoiceLines(id(r, "id")));
    }
    view(r, s, "billing");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    switch (p(r, "action")) {
      case "create" -> {
        long invoice = new BillingService().createInvoice(user(r), id(r, "recordId"));
        redirect(r, s, "/billing?id=" + invoice, "Đã lập hóa đơn.");
      }
      case "cash" -> {
        long invoice = id(r, "invoiceId");
        new BillingService().processCashPayment(user(r), invoice);
        redirect(r, s, "/billing?id=" + invoice, "Đã nhận thanh toán tiền mặt.");
      }
      case "vnpay" ->
          s.sendRedirect(
              new PaymentService().createLink(user(r), id(r, "invoiceId"), r.getRemoteAddr()));
      default -> throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
    }
  }
}
