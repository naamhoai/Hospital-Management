package vn.hospital.service;

import java.sql.*;
import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.dao.InvoiceDAO;
import vn.hospital.model.AuthUser;
import vn.hospital.model.Invoice;
import vn.hospital.model.MedicalRecord;
import vn.hospital.util.*;

public class BillingService {
  /**
   * SQ10 - Lập hóa đơn. Đầu vào: nhân viên thu ngân và recordId. Đọc phí khám, chỉ định và đơn
   * thuốc chưa lên hóa đơn. Dùng giá đã chụp tại lúc phát sinh, không lấy lại đơn giá mới trong
   * danh mục. Đầu ra: invoiceId với invoice_line/tổng tiền; không thu lại dòng phí đã được tính ở
   * đợt trước.
   */
  public long createInvoice(AuthUser cashier, long record) {
    Actors.require(cashier, "CASHIER", "ADMIN");
    return Database.tx(
        connection -> createInvoice(connection, cashier.getUserId(), cashier.getUserId(), record));
  }

  public void generateFinalInvoice(Connection connection, AuthUser doctor, MedicalRecord record)
      throws SQLException {
    EMRService.editable(record, doctor, true);
    if (new InvoiceDAO().hasUnbilledCosts(connection, record.getRecordId()))
      createInvoice(connection, doctor.getUserId(), null, record.getRecordId());
  }

  /** Gom các phí chưa lên hóa đơn; không thu lại phí khám, xét nghiệm hay thuốc đã tính. */
  private long createInvoice(Connection connection, long actor, Long cashier, long record)
      throws SQLException {
    var records = new MedicalRecordDAO();
    var dao = new InvoiceDAO();
    var m = records.findByIdRow(connection, record);
    Checks.require(m != null, "Bệnh án không tồn tại.");
    var pending = dao.findPendingRows(connection, record);
    long invoice;
    if (pending != null) {
      invoice = Rows.number(pending, "invoice_id");
      if (new PaymentAttemptDAO().findPendingRows(connection, invoice) != null) {
        Checks.require(
            !dao.hasUnbilledCosts(connection, record),
            "Đang có giao dịch VNPay. Hoàn tất hoặc chờ giao dịch hết hạn trước khi cộng chi phí"
                + " mới.");
        return invoice;
      }
    } else
      invoice =
          dao.insertInvoiceRow(
              connection,
              "INV-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(),
              record,
              m.get("patient_id"),
              cashier);
    dao.addConsultationLine(connection, invoice, m.get("appointment_id"));
    dao.addClinicalLines(connection, invoice, record);
    dao.addMedicationLines(connection, invoice, record);
    var amount = dao.calculateTotalRows(connection, invoice);
    Checks.require(Rows.number(amount, "n") > 0, "Không còn chi phí mới cần lập hóa đơn.");
    dao.updateTotal(connection, amount.get("total"), invoice);
    AuditDAO.record(connection, actor, "CREATE_INVOICE", "invoice", invoice);
    return invoice;
  }

  /**
   * SQ10/WF01 - Xác nhận thu tiền mặt. Đầu vào: thu ngân của phiên và invoiceId. Chặn hóa đơn đã
   * trả hoặc đang có giao dịch VNPay. Đầu ra: hóa đơn PAID, các chỉ định/đơn thuộc hóa đơn được
   * đánh dấu PAID và lưu thông báo. Chỉ sau bước này Lab mới được duyệt kết quả.
   */
  public void processCashPayment(AuthUser cashier, long invoice) {
    Actors.require(cashier, "CASHIER", "ADMIN");
    Database.tx(
        connection -> {
          var i = new InvoiceDAO().findById(connection, invoice);
          Checks.require(i != null, "Hóa đơn không tồn tại.");
          Checks.require("UNPAID".equals(i.get("status")), "Hóa đơn đã thanh toán.");
          Checks.require(
              new PaymentAttemptDAO().findPendingRows(connection, invoice) == null,
              "Đang có giao dịch VNPay còn hiệu lực. Chờ hết 15 phút trước khi trả tiền mặt.");
          completePayment(
              connection, invoice, "CASH", "CASH-" + UUID.randomUUID(), cashier.getUserId());
          return null;
        });
  }

  /**
   * SQ10 - Cập nhật kết quả thu tiền đã được xác nhận. Nhận Connection của transaction, invoiceId,
   * phương thức, mã giao dịch và người xử lý. Đánh dấu hóa đơn và đúng các nguồn chi phí thuộc hóa
   * đơn đó, tránh đánh dấu nhầm toàn bệnh án.
   */
  public static void completePayment(
      Connection connection, long invoice, String method, String transaction, Long actor)
      throws SQLException {
    Checks.require(
        new InvoiceDAO().markPaid(connection, method, transaction, invoice) == 1,
        "Hóa đơn đã được xử lý.");
    new ClinicalOrderDAO().markPaidByInvoice(connection, invoice);
    new PrescriptionDAO().markPaidByInvoice(connection, invoice);
    var i = new InvoiceDAO().findPatientRows(connection, invoice);
    AppointmentService.notify(
        connection,
        Rows.number(i, "patient_id"),
        "Xác nhận thanh toán",
        "Đã thanh toán hóa đơn #" + invoice + " qua " + method);
    AuditDAO.record(connection, actor, "PAY", "invoice", invoice);
  }

  public List<Map<String, Object>> findInvoiceLines(Object... values) {
    return new InvoiceDAO().findInvoiceLines(values);
  }

  public List<Map<String, Object>> findInvoiceDetails(Object... values) {
    return new InvoiceDAO().findInvoiceDetails(values);
  }

  public List<Map<String, Object>> findBillableRecords(Object... values) {
    return new InvoiceDAO().findBillableRecords(values);
  }

  public List<Map<String, Object>> getInvoices() {
    return new InvoiceDAO().list();
  }

  /** Bỏ phí chỉ định đã hủy và tính lại hóa đơn trong transaction của thao tác hủy. */
  public void removeUnpaidClinicalCharge(
      Connection connection, Invoice invoice, int orderId, long actorId) throws SQLException {
    if (invoice == null) return; // Chưa lên hóa đơn thì không có khoản phí cần xóa.
    Checks.require(
        "UNPAID".equals(invoice.getStatus()),
        "Không được hủy chi phí của hóa đơn đã thanh toán hoặc đã khóa.");
    var payments = new PaymentAttemptDAO();
    Checks.require(
        payments.findPendingRows(connection, invoice.getInvoiceId()) == null,
        "Đang có giao dịch VNPay. Chờ giao dịch kết thúc trước khi hủy chỉ định.");
    // Callback của giao dịch cũ không được phép thanh toán số tiền sau khi điều chỉnh.
    payments.expirePendingAttempts(connection, invoice.getInvoiceId());
    var invoices = new InvoiceDAO();
    invoices.removeClinicalLine(connection, invoice.getInvoiceId(), orderId);
    invoices.recalculateAfterCancellation(connection, invoice.getInvoiceId());
    AuditDAO.record(connection, actorId, "REMOVE_ORDER_CHARGE", "invoice", invoice.getInvoiceId());
  }
}
