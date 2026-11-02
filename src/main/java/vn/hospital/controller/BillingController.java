package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.util.BusinessException;
import vn.hospital.util.Checks;

@WebServlet("/billing")
public class BillingController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setAttribute("invoices", new BillingService().getInvoices());
    request.setAttribute("records", new BillingService().findBillableRecords());
    if (!param(request, "id").isBlank()) {
      var rows = new BillingService().findInvoiceDetails(id(request, "id"));
      Checks.require(!rows.isEmpty(), "Không tìm thấy hóa đơn.");
      request.setAttribute("invoice", rows.get(0));
      request.setAttribute("lines", new BillingService().findInvoiceLines(id(request, "id")));
    }
    view(request, response, "billing");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    switch (param(request, "action")) {
      case "create" -> {
        long invoice = new BillingService().createInvoice(user(request), id(request, "recordId"));
        redirect(request, response, "/billing?id=" + invoice, "Đã lập hóa đơn.");
      }
      case "cash" -> {
        long invoice = id(request, "invoiceId");
        new BillingService().processCashPayment(user(request), invoice);
        redirect(request, response, "/billing?id=" + invoice, "Đã nhận thanh toán tiền mặt.");
      }
      case "vnpay" ->
          response.sendRedirect(
              new PaymentService()
                  .createLink(user(request), id(request, "invoiceId"), request.getRemoteAddr()));
      default -> throw new BusinessException("Thao tác không hợp lệ.");
    }
  }
}
