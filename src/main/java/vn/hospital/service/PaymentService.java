package vn.hospital.service;

import java.math.BigDecimal;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.integration.VNPayClient;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class PaymentService {
  /** Tạo mã giao dịch và URL thanh toán cho đúng chủ hóa đơn; chưa cập nhật PAID tại đây. */
  public String createLink(AuthUser cashier, long invoice, String ip) {
    Actors.require(cashier, "CASHIER", "ADMIN", "PATIENT");
    return Database.tx(
        connection -> {
          var i = new InvoiceDAO().findById(connection, invoice);
          Checks.require(
              i != null && "UNPAID".equals(i.get("status")), "Hóa đơn không hợp lệ hoặc đã trả.");
          Checks.require(
              !cashier.is("PATIENT") || Rows.number(i, "patient_id") == cashier.getUserId(),
              "Bạn không được thanh toán hóa đơn này.");
          var existing = new PaymentAttemptDAO().findLatestPendingRows(connection, invoice);
          Checks.require(
              existing == null, "Đã tạo giao dịch VNPay. Chờ hết hạn 15 phút trước khi tạo lại.");
          String ref = UUID.randomUUID().toString().replace("-", "");
          String url =
              new VNPayClient().createPaymentUrl(ref, (BigDecimal) i.get("total_amount"), ip);
          new PaymentAttemptDAO().insertAttempt(connection, ref, invoice, i.get("total_amount"));
          AuditDAO.record(connection, cashier.getUserId(), "CREATE_VNPAY", "invoice", invoice);
          return url;
        });
  }

  /** Chỉ nhận kết quả khi đúng chữ ký/số tiền; callback lặp không cộng tiền lần hai. */
  public String handleCallback(Map<String, String> params) {
    Checks.require(new VNPayClient().verifyCallback(params), "Chữ ký VNPay không hợp lệ.");
    Checks.require(
        Config.get("VNPAY_TMN_CODE").equals(params.get("vnp_TmnCode")), "Mã đơn vị không hợp lệ.");
    return Database.tx(
        connection -> {
          // Khóa hóa đơn trước giao dịch cổng thanh toán, cùng thứ tự với thu ngân để tránh
          // deadlock.
          var found = new PaymentAttemptDAO().findInvoiceRows(connection, params.get("vnp_TxnRef"));
          Checks.require(found != null, "Không tìm thấy giao dịch.");
          long invoice = Rows.number(found, "invoice_id");
          var i = new InvoiceDAO().findById(connection, invoice);
          var a = new PaymentAttemptDAO().lockAttemptRows(connection, params.get("vnp_TxnRef"));
          BigDecimal amount;
          try {
            amount = new BigDecimal(params.get("vnp_Amount")).movePointLeft(2);
          } catch (Exception e) {
            throw new BusinessException("Số tiền callback không hợp lệ.");
          }
          Checks.require(
              amount.compareTo((BigDecimal) a.get("amount")) == 0
                  && amount.compareTo((BigDecimal) i.get("total_amount")) == 0,
              "Số tiền VNPay không khớp hóa đơn.");
          Checks.require(
              "PENDING".equals(a.get("status")) || "SUCCESS".equals(a.get("status")),
              "Giao dịch đã hết hiệu lực hoặc hóa đơn đã điều chỉnh; cần đối soát với cổng thanh"
                  + " toán.");
          boolean success =
              "00".equals(params.get("vnp_ResponseCode"))
                  && "00".equals(params.get("vnp_TransactionStatus"));
          if ("SUCCESS".equals(a.get("status"))) return "Hóa đơn đã được xác nhận trước đó.";
          if (!success) {
            new PaymentAttemptDAO().markFailed(connection, params.get("vnp_TxnRef"));
            return "Thanh toán chưa thành công. Hóa đơn vẫn chưa thanh toán.";
          }
          Checks.require(
              "UNPAID".equals(i.get("status")),
              "Hóa đơn đã được thanh toán bằng giao dịch khác; cần đối soát.");
          String transaction =
              Checks.text(params.get("vnp_TransactionNo"), 100, "Mã giao dịch VNPay");
          BillingService.completePayment(
              connection, invoice, "VNPAY", "VNPAY-" + transaction, null);
          new PaymentAttemptDAO().markSucceeded(connection, transaction, params.get("vnp_TxnRef"));
          return "Thanh toán thành công. Hóa đơn #" + invoice;
        });
  }
}
