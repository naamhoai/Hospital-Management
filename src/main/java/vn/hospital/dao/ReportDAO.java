package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.model.*;
import vn.hospital.util.Database;

/** Read-only aggregation of operational/medical and paid billing data (CD12). */
public class ReportDAO {
  private String period(String column, String group) {
    return switch (group) {
      case "MONTH" -> "DATE_FORMAT(" + column + ",'%Y-%m')";
      case "QUARTER" -> "CONCAT(YEAR(" + column + "),'-Q',QUARTER(" + column + "))";
      default -> "DATE_FORMAT(" + column + ",'%Y-%m-%d')";
    };
  }

  private Object[] args(ReportFilter f) {
    return new Object[] {
      f.getFromDate(),
      f.getToDate(),
      f.getDepartmentId(),
      f.getDepartmentId(),
      f.getServiceId(),
      f.getServiceId(),
      f.getPaymentMethod(),
      f.getPaymentMethod()
    };
  }

  public PatientFlowReport queryPatientFlowMetrics(ReportFilter filter) {
    return Database.read(
        c -> {
          String where =
              " FROM appointment a JOIN doctors d ON d.doctor_id=a.doctor_id WHERE"
                  + " a.appointment_date BETWEEN ? AND ? AND (? IS NULL OR d.department_id=?) AND"
                  + " (? IS NULL OR EXISTS(SELECT 1 FROM medical_record m JOIN clinical_order o ON"
                  + " o.record_id=m.record_id WHERE m.appointment_id=a.appointment_id AND"
                  + " o.service_id=? AND o.status<>'CANCELLED')) AND (?='' OR EXISTS(SELECT 1 FROM"
                  + " medical_record m JOIN invoice i ON i.record_id=m.record_id WHERE"
                  + " m.appointment_id=a.appointment_id AND i.status='PAID' AND"
                  + " i.payment_method=?))";
          String metrics =
              "COUNT(*) patient_count,COALESCE(SUM(a.status='COMPLETED'),0)"
                  + " completed_count,COALESCE(SUM(a.status='CANCELLED'),0)"
                  + " cancelled_count,COALESCE(SUM(a.status='NO_SHOW'),0) no_show_count";
          PatientFlowReport report =
              BeanMapper.map(
                  Jdbc.one(c, "SELECT " + metrics + where, args(filter)), PatientFlowReport.class);
          String group = period("a.appointment_date", filter.getGroupBy());
          List<PatientFlowRow> rows =
              BeanMapper.list(
                  Jdbc.list(
                      c,
                      "SELECT "
                          + group
                          + " label,"
                          + metrics
                          + where
                          + " GROUP BY label ORDER BY label",
                      args(filter)),
                  PatientFlowRow.class);
          report.setRows(rows);
          report.setFromDate(filter.getFromDate());
          report.setToDate(filter.getToDate());
          return report;
        });
  }

  public RevenueReport queryRevenueMetrics(ReportFilter filter) {
    return Database.read(
        c -> {
          Object[] revenueArgs = java.util.Arrays.copyOf(args(filter), 10);
          revenueArgs[8] = filter.getServiceType();
          revenueArgs[9] = filter.getServiceType();
          String where =
              " FROM invoice i JOIN invoice_line l ON l.invoice_id=i.invoice_id JOIN medical_record"
                  + " m ON m.record_id=i.record_id JOIN doctors doc ON doc.doctor_id=m.doctor_id"
                  + " LEFT JOIN clinical_order co ON l.source_type='ORDER' AND"
                  + " co.order_id=l.source_id LEFT JOIN service srv ON srv.service_id=co.service_id"
                  + " JOIN departments d ON"
                  + " d.department_id=COALESCE(srv.department_id,doc.department_id) WHERE"
                  + " i.status='PAID' AND i.paid_at>=? AND i.paid_at<DATE_ADD(?,INTERVAL 1 DAY) AND"
                  + " (? IS NULL OR d.department_id=?) AND (? IS NULL OR srv.service_id=?) AND"
                  + " (?='' OR i.payment_method=?) AND (?='' OR l.source_type=?)";
          RevenueReport report =
              BeanMapper.map(
                  Jdbc.one(
                      c,
                      "SELECT COUNT(DISTINCT i.invoice_id)"
                          + " paid_invoice_count,COALESCE(SUM(l.quantity*l.unit_price),0)"
                          + " total_revenue"
                          + where,
                      revenueArgs),
                  RevenueReport.class);
          String group = period("i.paid_at", filter.getGroupBy());
          var rows =
              BeanMapper.list(
                  Jdbc.list(
                      c,
                      "SELECT "
                          + group
                          + " label,d.name department,CASE WHEN l.source_type='ORDER' THEN srv.name"
                          + " WHEN l.source_type='CONSULTATION' THEN 'Khám bệnh' ELSE 'Thuốc' END"
                          + " service,i.payment_method,COUNT(DISTINCT i.invoice_id)"
                          + " invoice_count,SUM(l.quantity*l.unit_price) total_revenue"
                          + where
                          + " GROUP BY label,d.name,service,i.payment_method ORDER BY"
                          + " label,department,service",
                      revenueArgs),
                  RevenueRow.class);
          report.setRows(rows);
          report.setFromDate(filter.getFromDate());
          report.setToDate(filter.getToDate());
          report.setPaymentMethod(filter.getPaymentMethod());
          return report;
        });
  }

  public List<Map<String, Object>> filterDepartments() {
    return Jdbc.read(
        "SELECT department_id,name FROM departments WHERE status='ACTIVE' ORDER BY name");
  }

  public List<Map<String, Object>> filterServices() {
    return Jdbc.read(
        "SELECT service_id,name FROM service WHERE category<>'CONSULTATION' ORDER BY name");
  }

  /** Compatibility bridge for the original group integration test. */
  public List<Map<String, Object>> queryRevenueMetrics(
      java.time.LocalDate from, java.time.LocalDate to, String method) {
    var f = new ReportFilter();
    f.setFromDate(from);
    f.setToDate(to);
    f.setGroupBy("DAY");
    f.setPaymentMethod(method);
    return queryRevenueMetrics(f).getRows().stream()
        .map(
            row -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("total_revenue", row.getTotalRevenue());
              return m;
            })
        .toList();
  }
}
