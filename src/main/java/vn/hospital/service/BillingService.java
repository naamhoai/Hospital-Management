package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
public class BillingService {
    public long createInvoice(AuthUser cashier,long record) {
        return Database.tx(c -> {
            var m=new MedicalRecordDAO().findById(c,record); Checks.require(m!=null,"Bệnh án không tồn tại.");
            var pending=Jdbc.one(c,"SELECT invoice_id FROM invoice WHERE record_id=? AND status='UNPAID'",record); if(pending!=null) return Jdbc.number(pending,"invoice_id");
            long invoice=Jdbc.insert(c,"INSERT INTO invoice(invoice_code,record_id,patient_id,cashier_id,total_amount) VALUES(?,?,?,?,0)","INV-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(),record,m.get("patient_id"),cashier.getUserId());
            Jdbc.update(c,"INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price) SELECT ?,'CONSULTATION',a.appointment_id,'Phi kham',1,a.consultation_fee FROM appointment a WHERE a.appointment_id=? AND NOT EXISTS(SELECT 1 FROM invoice_line l WHERE l.source_type='CONSULTATION' AND l.source_id=a.appointment_id)",invoice,m.get("appointment_id"));
            Jdbc.update(c,"INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price) SELECT ?,'ORDER',o.order_id,s.name,1,o.unit_price FROM clinical_order o JOIN service s USING(service_id) WHERE o.record_id=? AND NOT EXISTS(SELECT 1 FROM invoice_line l WHERE l.source_type='ORDER' AND l.source_id=o.order_id)",invoice,record);
            Jdbc.update(c,"INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price) SELECT ?,'MEDICATION',pd.id,m.name,pd.quantity,pd.unit_price FROM prescription_detail pd JOIN prescription p USING(prescription_id) JOIN medication m USING(medication_id) WHERE p.record_id=? AND NOT EXISTS(SELECT 1 FROM invoice_line l WHERE l.source_type='MEDICATION' AND l.source_id=pd.id)",invoice,record);
            var amount=Jdbc.one(c,"SELECT COUNT(*) n,COALESCE(SUM(quantity*unit_price),0) total FROM invoice_line WHERE invoice_id=?",invoice);
            Checks.require(Jdbc.number(amount,"n")>0,"Không còn chi phí mới cần lập hóa đơn.");
            Jdbc.update(c,"UPDATE invoice SET total_amount=? WHERE invoice_id=?",amount.get("total"),invoice); AuditDAO.record(c,cashier.getUserId(),"CREATE_INVOICE","invoice",invoice); return invoice;
        });
    }
    public void processCashPayment(AuthUser cashier,long invoice) {
        Database.tx(c -> {
            var i=new InvoiceDAO().findById(c,invoice); Checks.require(i!=null,"Hóa đơn không tồn tại.");
            Checks.require("UNPAID".equals(i.get("status")),"Hóa đơn đã thanh toán.");
            Checks.require(Jdbc.one(c,"SELECT attempt_id FROM payment_attempt WHERE invoice_id=? AND status='PENDING' AND created_at>DATE_SUB(NOW(),INTERVAL 15 MINUTE)",invoice)==null,"Đang có giao dịch VNPay còn hiệu lực. Chờ hết 15 phút trước khi trả tiền mặt.");
            completePayment(c,invoice,"CASH","CASH-"+UUID.randomUUID(),cashier.getUserId()); return null;
        });
    }
    public static void completePayment(Connection c,long invoice,String method,String transaction,Long actor) throws SQLException {
        Checks.require(Jdbc.update(c,"UPDATE invoice SET status='PAID',payment_method=?,transaction_id=?,paid_at=NOW() WHERE invoice_id=? AND status='UNPAID'",method,transaction,invoice)==1,"Hóa đơn đã được xử lý.");
        Jdbc.update(c,"UPDATE clinical_order SET payment_status='PAID' WHERE order_id IN (SELECT source_id FROM invoice_line WHERE invoice_id=? AND source_type='ORDER')",invoice);
        Jdbc.update(c,"UPDATE prescription p SET payment_status='PAID' WHERE p.prescription_id IN (SELECT pd.prescription_id FROM prescription_detail pd JOIN invoice_line l ON l.source_type='MEDICATION' AND l.source_id=pd.id WHERE l.invoice_id=?)",invoice);
        var i=Jdbc.one(c,"SELECT patient_id FROM invoice WHERE invoice_id=?",invoice); AppointmentService.notify(c,Jdbc.number(i,"patient_id"),"Xác nhận thanh toán","Đã thanh toán hóa đơn #"+invoice+" qua "+method); AuditDAO.record(c,actor,"PAY","invoice",invoice);
    }
}
