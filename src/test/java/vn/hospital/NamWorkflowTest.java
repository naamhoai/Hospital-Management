package vn.hospital;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.hospital.dao.*;
import vn.hospital.model.*;
import vn.hospital.service.*;
import vn.hospital.util.*;

@EnabledIfEnvironmentVariable(named = "HOSPITAL_INTEGRATION", matches = "1")
class NamWorkflowTest {
  private final AuthUser doctor = new AuthUser(6, "Doctor", "DOCTOR"),
      other = new AuthUser(7, "Other", "DOCTOR"),
      lab = new AuthUser(5, "Lab", "LAB_TECHNICIAN"),
      cashier = new AuthUser(3, "Cashier", "CASHIER"),
      manager = new AuthUser(2, "Manager", "MANAGER");
  private int queue, record;

  @BeforeEach
  void fixture() {
    Database.tx(
        c -> {
          assertEquals("hospital_test", c.getCatalog());
          long next =
              Jdbc.number(
                  Jdbc.one(c, "SELECT COALESCE(MAX(appointment_id),0)+1 n FROM appointment"), "n");
          long schedule =
              Jdbc.insert(
                  c,
                  "INSERT INTO"
                      + " work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots)"
                      + " VALUES(6,CURDATE(),?,'00:00','23:59',100)",
                  "N" + UUID.randomUUID().toString().substring(0, 12));
          long appt =
              Jdbc.insert(
                  c,
                  "INSERT INTO"
                      + " appointment(appointment_code,patient_id,doctor_id,schedule_id,appointment_date,appointment_time,consultation_fee,status)"
                      + " VALUES(?,8,6,?,CURDATE(),?,150000,'CHECKED_IN')",
                  "NT" + UUID.randomUUID(),
                  schedule,
                  LocalTime.ofSecondOfDay((next * 37) % 86000));
          queue =
              Actors.id(
                  Jdbc.insert(
                      c,
                      "INSERT INTO"
                          + " queue(appointment_id,patient_id,doctor_id,work_date,queue_number,room_assigned,status)"
                          + " VALUES(?,8,6,CURDATE(),?,'P101','IN_CONSULTATION')",
                      appt,
                      100000 + next));
          record =
              Actors.id(
                  Jdbc.insert(
                      c,
                      "INSERT INTO medical_record(appointment_id,patient_id,doctor_id,diagnosis)"
                          + " VALUES(?,8,6,'')",
                      appt));
          return null;
        });
    new EMRService(doctor)
        .recordClinicalDiagnosis(
            queue, new DiagnosisRequest("Sốt nhẹ", "J00", "Chẩn đoán thử nghiệm", "Ghi chú"));
  }

  @AfterEach
  void releaseConsultation() {
    if (queue > 0)
      Database.tx(
          c -> {
            Jdbc.update(
                c,
                "UPDATE appointment a JOIN queue q ON q.appointment_id=a.appointment_id SET"
                    + " a.status='COMPLETED' WHERE q.queue_id=?",
                queue);
            Jdbc.update(
                c,
                "UPDATE appointment a JOIN queue q ON q.appointment_id=a.appointment_id SET"
                    + " a.status='COMPLETED' WHERE q.queue_id=?",
                queue);
            Jdbc.update(c, "UPDATE queue SET status='COMPLETED' WHERE queue_id=?", queue);
            return null;
          });
  }

  private ReportFilter filter() {
    var f = new ReportFilter();
    f.setFromDate(LocalDate.now());
    f.setToDate(LocalDate.now());
    f.setGroupBy("DAY");
    f.setPaymentMethod("CASH");
    return f;
  }

  @Test
  void cd10HistoryAndPrescriptionRemainOwnedAndAtomic() throws Exception {
    assertEquals(record, new EMRService(doctor).getRecord(queue).getRecordId());
    assertNotNull(
        new PatientRecordService(new AuthUser(8, "Patient", "PATIENT"))
            .workspace(record)
            .get("record"));
    assertThrows(
        BusinessException.class,
        () -> new PatientRecordService(new AuthUser(9, "Other", "PATIENT")).workspace(record));
    assertTrue(
        new EMRService(doctor)
            .getPatientHistory(8).stream().anyMatch(r -> r.getRecordId() == record));
    assertThrows(BusinessException.class, () -> new EMRService(other).getRecord(queue));
    long stock =
        Jdbc.number(
            Jdbc.read("SELECT stock_quantity FROM medication WHERE medication_id=1").get(0),
            "stock_quantity");
    var line = new PrescriptionDetail();
    line.setMedicationId(1);
    line.setQuantity(2);
    line.setDosage("Liều dùng thử");
    var req = new PrescriptionRequest();
    req.setRecordId(record);
    req.setItems(List.of(line));
    req.setAdvice("Theo dõi triệu chứng");
    assertTrue(new PrescriptionService(doctor).validatePrescription(req));
    var rx = new PrescriptionService(doctor).issuePrescription(req);
    assertEquals(2, rx.getItems().get(0).getQuantity());
    assertEquals(
        stock - 2,
        Jdbc.number(
            Jdbc.read("SELECT stock_quantity FROM medication WHERE medication_id=1").get(0),
            "stock_quantity"));
    assertThrows(
        BusinessException.class,
        () -> new PrescriptionService(other).prescriptionPage(rx.getPrescriptionId()));
    File pdf = new PrescriptionService(doctor).printPrescription(rx.getPrescriptionId());
    try {
      assertTrue(
          Files.readString(pdf.toPath(), java.nio.charset.StandardCharsets.ISO_8859_1)
              .startsWith("%PDF"));
    } finally {
      Files.delete(pdf.toPath());
    }
    new EMRService(doctor).closeRecord(queue);
    assertFalse(
        Jdbc.read("SELECT invoice_id FROM invoice WHERE record_id=? AND status='UNPAID'", record)
            .isEmpty());
  }

  @Test
  void cd11CancelIsExcludedFromBillingAndCannotCancelBilledOrders() {
    var service = new ClinicalService(doctor);
    var orders =
        service.createClinicalOrders(new ClinicalOrderRequest(record, List.of(2, 3), "Chỉ dẫn"));
    var cancelled = orders.stream().filter(o -> o.getServiceId() == 3).findFirst().orElseThrow();
    var payable = orders.stream().filter(o -> o.getServiceId() == 2).findFirst().orElseThrow();
    assertThrows(
        BusinessException.class,
        () -> new ClinicalService(other).cancelOrder(cancelled.getOrderId()));
    assertTrue(service.cancelOrder(cancelled.getOrderId()));
    var rxRequest = new PrescriptionRequest();
    rxRequest.setRecordId(record);
    var medication = new PrescriptionDetail();
    medication.setMedicationId(1);
    medication.setQuantity(1);
    medication.setDosage("Test");
    rxRequest.setItems(List.of(medication));
    assertThrows(
        BusinessException.class,
        () -> new PrescriptionService(doctor).issuePrescription(rxRequest));
    long invoice = new BillingService().createInvoice(cashier, record);
    assertEquals(
        240000,
        Jdbc.number(
            Jdbc.read("SELECT total_amount FROM invoice WHERE invoice_id=?", invoice).get(0),
            "total_amount"));
    assertThrows(BusinessException.class, () -> service.cancelOrder(payable.getOrderId()));
  }

  @Test
  void cd11UploadsAreAuthorizedAndResultsLockAfterApproval() throws Exception {
    var order =
        new ClinicalService(doctor)
            .createClinicalOrders(new ClinicalOrderRequest(record, List.of(2), "Theo dõi"))
            .get(0);
    var result = new TestResult();
    result.setResultData("Dữ liệu xét nghiệm thử");
    result.setConclusion("Kết luận thử");
    var tech = new ClinicalService(lab);
    assertThrows(BusinessException.class, () -> tech.recordTestResult(order.getOrderId(), result));
    long invoice = new BillingService().createInvoice(cashier, record);
    new BillingService().processCashPayment(cashier, invoice);
    var out = new ByteArrayOutputStream();
    new ReportExportService()
        .exportPDF("Kết quả thử", List.of(Map.of("Kết luận", "Hoàn tất")), out);
    var upload = new UploadData("result.pdf", "application/pdf", out.toByteArray());
    var saved = tech.recordTestResult(order.getOrderId(), result, upload);
    File file = tech.getAttachment(saved.getResultId());
    try {
      assertTrue(file.isFile());
      assertEquals(out.size(), file.length());
      assertTrue(new ClinicalService(doctor).getAttachment(saved.getResultId()).isFile());
      assertTrue(
          new ClinicalService(new AuthUser(8, "Patient", "PATIENT"))
              .getAttachment(saved.getResultId())
              .isFile());
      assertThrows(
          BusinessException.class,
          () -> new ClinicalService(other).getAttachment(saved.getResultId()));
      assertThrows(
          BusinessException.class,
          () ->
              new ClinicalService(new AuthUser(9, "Other", "PATIENT"))
                  .getAttachment(saved.getResultId()));
      assertThrows(
          BusinessException.class, () -> tech.recordTestResult(order.getOrderId(), result));
      new EMRService(doctor).closeRecord(queue);
      assertEquals("CLOSED", new EMRService(doctor).getRecord(queue).getStatus());
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }

  @Test
  void cd12RevenueFiltersAndGroupingDoNotCountUnpaidInvoices() {
    var service = new AnalyticsService(manager);
    var f = filter();
    f.setServiceId(4);
    BigDecimal before = service.getRevenueReport(f).getTotalRevenue();
    new ClinicalService(doctor)
        .createClinicalOrders(new ClinicalOrderRequest(record, List.of(4), ""));
    long invoice = new BillingService().createInvoice(cashier, record);
    assertEquals(0, before.compareTo(service.getRevenueReport(f).getTotalRevenue()));
    new BillingService().processCashPayment(cashier, invoice);
    assertEquals(
        0,
        new BigDecimal("200000")
            .compareTo(service.getRevenueReport(f).getTotalRevenue().subtract(before)));
    f.setDepartmentId(1);
    assertEquals(BigDecimal.ZERO.setScale(2), service.getRevenueReport(f).getTotalRevenue());
    f.setDepartmentId(2);
    assertTrue(
        service.getRevenueReport(f).getTotalRevenue().compareTo(new BigDecimal("200000")) >= 0);
    f.setPaymentMethod("VNPAY");
    assertEquals(0, service.getRevenueReport(f).getTotalRevenue().compareTo(BigDecimal.ZERO));
    f.setPaymentMethod("CASH");
    f.setServiceType("MEDICATION");
    f.setServiceId(null);
    assertEquals(0, service.getRevenueReport(f).getTotalRevenue().compareTo(BigDecimal.ZERO));
    f.setServiceType("ORDER");
    f.setServiceId(4);
    for (String group : List.of("DAY", "MONTH", "QUARTER")) {
      f.setGroupBy(group);
      var report = service.getRevenueReport(f);
      assertFalse(report.getSeries().isEmpty());
      assertTrue(
          report
              .getSeries()
              .get(0)
              .getLabel()
              .startsWith(String.valueOf(LocalDate.now().getYear())));
    }
  }

  @Test
  void cd12EmptyFlowAndExportsAreWellFormed() throws Exception {
    var f = filter();
    f.setFromDate(LocalDate.of(2001, 1, 1));
    f.setToDate(LocalDate.of(2001, 1, 2));
    f.setPaymentMethod("");
    var service = new AnalyticsService(manager);
    var report = service.getPatientFlowReport(f);
    assertEquals(0, report.getPatientCount());
    assertEquals(0, report.getCompletionRate().compareTo(BigDecimal.ZERO));
    assertTrue(report.getSeries().isEmpty());
    for (String format : List.of("pdf", "xlsx")) {
      File file = service.exportReport("flow", f, format);
      try {
        assertTrue(file.length() > 100);
      } finally {
        Files.delete(file.toPath());
      }
    }
  }

  @Test
  void concurrentResultApprovalCreatesExactlyOneResult() throws Exception {
    var order =
        new ClinicalService(doctor)
            .createClinicalOrders(
                new ClinicalOrderRequest(record, List.of(2), "Concurrent approval"))
            .get(0);
    long invoice = new BillingService().createInvoice(cashier, record);
    new BillingService().processCashPayment(cashier, invoice);
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    var gate = new java.util.concurrent.CountDownLatch(1);
    try {
      List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();
      for (int n = 0; n < 2; n++)
        futures.add(
            executor.submit(
                () -> {
                  gate.await();
                  var data = new TestResult();
                  data.setResultData("Concurrent result");
                  try {
                    new ClinicalService(lab).recordTestResult(order.getOrderId(), data);
                    return true;
                  } catch (BusinessException e) {
                    return false;
                  }
                }));
      gate.countDown();
      int success = 0;
      for (var f : futures) if (f.get(15, java.util.concurrent.TimeUnit.SECONDS)) success++;
      assertEquals(1, success);
      assertEquals(
          1,
          Jdbc.number(
              Jdbc.read("SELECT COUNT(*) n FROM test_result WHERE order_id=?", order.getOrderId())
                  .get(0),
              "n"));
    } finally {
      executor.shutdownNow();
    }
  }
}
