package vn.hospital.service;

import java.sql.*;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public class BillingService {
  public long createInvoice(AuthUser cashier, long record) {
    Actors.require(cashier, "CASHIER", "ADMIN");
    return Database.tx(c -> createInvoice(c, cashier.getUserId(), cashier.getUserId(), record));
  }

  public void generateFinalInvoice(
      Connection c, AuthUser doctor, vn.hospital.model.MedicalRecord record) throws SQLException {
    EMRService.editable(record, doctor, true);
    if (new InvoiceDAO().hasUnbilledCosts(c, record.getRecordId()))
      createInvoice(c, doctor.getUserId(), null, record.getRecordId());
  }

  private long createInvoice(Connection c, long actor, Long cashier, long record)
      throws SQLException {
    var records = new MedicalRecordDAO();
    var dao = new InvoiceDAO();
    var m = records.findByIdRow(c, record);
    Checks.require(m != null, "Bệnh án không tồn tại.");
    var pending = dao.findPendingRows(c, record);
    long invoice;
    if (pending != null) {
      invoice = Rows.number(pending, "invoice_id");
      if (new PaymentAttemptDAO().findPendingRows(c, invoice) != null) {
        Checks.require(
            !dao.hasUnbilledCosts(c, record),
            "Đang có giao dịch VNPay. Hoàn tất hoặc chờ giao dịch hết hạn trước khi cộng chi phí"
                + " mới.");
        return invoice;
      }
    } else
      invoice =
          dao.insertInvoiceRow(
              c,
              "INV-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(),
              record,
              m.get("patient_id"),
              cashier);
    dao.addConsultationLine(c, invoice, m.get("appointment_id"));
    dao.addClinicalLines(c, invoice, record);
    dao.addMedicationLines(c, invoice, record);
    var amount = dao.calculateTotalRows(c, invoice);
    Checks.require(Rows.number(amount, "n") > 0, "Không còn chi phí mới cần lập hóa đơn.");
    dao.updateTotal(c, amount.get("total"), invoice);
    AuditDAO.record(c, actor, "CREATE_INVOICE", "invoice", invoice);
    return invoice;
  }

  public void processCashPayment(AuthUser cashier, long invoice) {
    Actors.require(cashier, "CASHIER", "ADMIN");
    Database.tx(
        c -> {
          var i = new InvoiceDAO().findById(c, invoice);
          Checks.require(i != null, "Hóa đơn không tồn tại.");
          Checks.require("UNPAID".equals(i.get("status")), "Hóa đơn đã thanh toán.");
          Checks.require(
              new PaymentAttemptDAO().findPendingRows(c, invoice) == null,
              "Đang có giao dịch VNPay còn hiệu lực. Chờ hết 15 phút trước khi trả tiền mặt.");
          completePayment(c, invoice, "CASH", "CASH-" + UUID.randomUUID(), cashier.getUserId());
          return null;
        });
  }

  public static void completePayment(
      Connection c, long invoice, String method, String transaction, Long actor)
      throws SQLException {
    Checks.require(
        new InvoiceDAO().markPaid(c, method, transaction, invoice) == 1, "Hóa đơn đã được xử lý.");
    new ClinicalOrderDAO().markPaidByInvoice(c, invoice);
    new PrescriptionDAO().markPaidByInvoice(c, invoice);
    var i = new InvoiceDAO().findPatientRows(c, invoice);
    AppointmentService.notify(
        c,
        Rows.number(i, "patient_id"),
        "Xác nhận thanh toán",
        "Đã thanh toán hóa đơn #" + invoice + " qua " + method);
    AuditDAO.record(c, actor, "PAY", "invoice", invoice);
  }

  public java.util.List<java.util.Map<String, Object>> findInvoiceLines(Object... values) {
    return new vn.hospital.dao.InvoiceDAO().findInvoiceLines(values);
  }

  public java.util.List<java.util.Map<String, Object>> findInvoiceDetails(Object... values) {
    return new vn.hospital.dao.InvoiceDAO().findInvoiceDetails(values);
  }

  public java.util.List<java.util.Map<String, Object>> findBillableRecords(Object... values) {
    return new vn.hospital.dao.InvoiceDAO().findBillableRecords(values);
  }

  public java.util.List<java.util.Map<String, Object>> getInvoices() {
    return new InvoiceDAO().list();
  }

  /** Bỏ phí chỉ định đã hủy và tính lại hóa đơn trong transaction của thao tác hủy. */
  public void removeUnpaidClinicalCharge(
      Connection c, vn.hospital.model.Invoice invoice, int orderId, long actorId)
      throws SQLException {
    if (invoice == null) return; // Chưa lên hóa đơn thì không có khoản phí cần xóa.
    Checks.require(
        "UNPAID".equals(invoice.getStatus()),
        "Không được hủy chi phí của hóa đơn đã thanh toán hoặc đã khóa.");
    var payments = new PaymentAttemptDAO();
    Checks.require(
        payments.findPendingRows(c, invoice.getInvoiceId()) == null,
        "Đang có giao dịch VNPay. Chờ giao dịch kết thúc trước khi hủy chỉ định.");
    // Callback của giao dịch cũ không được phép thanh toán số tiền sau khi điều chỉnh.
    payments.expirePendingAttempts(c, invoice.getInvoiceId());
    var invoices = new InvoiceDAO();
    invoices.removeClinicalLine(c, invoice.getInvoiceId(), orderId);
    invoices.recalculateAfterCancellation(c, invoice.getInvoiceId());
    AuditDAO.record(c, actorId, "REMOVE_ORDER_CHARGE", "invoice", invoice.getInvoiceId());
  }
}
