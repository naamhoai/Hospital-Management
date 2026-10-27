package vn.hospital.dao;
import java.time.LocalDate;
import java.util.*;
public class ReportDAO {
    public List<Map<String,Object>> queryPatientFlowMetrics(LocalDate from,LocalDate to) {
        return Jdbc.read("SELECT appointment_date day,COUNT(*) patient_count,SUM(status='COMPLETED') completed_count,SUM(status='CANCELLED') cancelled_count,SUM(status='NO_SHOW') no_show_count,ROUND(100.0*SUM(status='COMPLETED')/COUNT(*),2) completion_rate,ROUND(100.0*SUM(status='CANCELLED')/COUNT(*),2) cancellation_rate,ROUND(100.0*SUM(status='NO_SHOW')/COUNT(*),2) no_show_rate FROM appointment WHERE appointment_date BETWEEN ? AND ? GROUP BY appointment_date ORDER BY appointment_date",from,to);
    }
    public List<Map<String,Object>> queryRevenueMetrics(LocalDate from,LocalDate to,String method) {
        return Jdbc.read("SELECT DATE(i.paid_at) day,d.name department,l.source_type category,i.payment_method,COUNT(DISTINCT i.invoice_id) invoice_count,SUM(l.quantity*l.unit_price) total_revenue FROM invoice i JOIN invoice_line l USING(invoice_id) JOIN medical_record m ON m.record_id=i.record_id JOIN doctors doc ON doc.doctor_id=m.doctor_id JOIN departments d ON d.department_id=doc.department_id WHERE i.status='PAID' AND i.paid_at>=? AND i.paid_at<DATE_ADD(?,INTERVAL 1 DAY) AND (?='' OR i.payment_method=?) GROUP BY DATE(i.paid_at),d.department_id,d.name,l.source_type,i.payment_method ORDER BY day,department,category",from,to,method,method);
    }
}
