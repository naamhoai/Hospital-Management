package vn.hospital;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.hospital.dao.*;
import vn.hospital.model.*;
import vn.hospital.service.*;
import vn.hospital.util.*;

@EnabledIfEnvironmentVariable(named = "HOSPITAL_INTEGRATION", matches = "1")
class NamRevisionTest {
  private final AuthUser doctor = new AuthUser(6, "Doctor", "DOCTOR");
  private final AuthUser lab = new AuthUser(5, "Lab", "LAB_TECHNICIAN");
  private final AuthUser cashier = new AuthUser(3, "Cashier", "CASHIER");
  private AuthUser head;
  private int recordId, queueId;
  private final List<String> filesToDelete = new ArrayList<>();

  @BeforeEach
  void createVisit() {
    Database.tx(
        c -> {
          assertEquals(
              "hospital_test",
              c.getCatalog(),
              "Only run against the disposable hospital_test database.");
          var account = Jdbc.one(c, "SELECT user_id FROM users WHERE email='head@hospital.test'");
          assertNotNull(account, "Run 05-nam-workflows.sql and 06-demo-department-head.sql first.");
          head = new AuthUser(Jdbc.number(account, "user_id"), "Head", "DEPARTMENT_HEAD");
          long next =
              Jdbc.number(
                  Jdbc.one(c, "SELECT COALESCE(MAX(appointment_id),0)+1 n FROM appointment"), "n");
          long schedule =
              Jdbc.insert(
                  c,
                  "INSERT INTO"
                      + " work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots)"
                      + " VALUES(6,CURDATE(),?,'00:00','23:59',100)",
                  "R" + UUID.randomUUID().toString().substring(0, 12));
          long appointment =
              Jdbc.insert(
                  c,
                  "INSERT INTO"
                      + " appointment(appointment_code,patient_id,doctor_id,schedule_id,appointment_date,appointment_time,consultation_fee,status)"
                      + " VALUES(?,8,6,?,CURDATE(),?,150000,'CHECKED_IN')",
                  "REV" + UUID.randomUUID(),
                  schedule,
                  LocalTime.ofSecondOfDay((next * 37) % 86000));
          queueId =
              Actors.id(
                  Jdbc.insert(
                      c,
                      "INSERT INTO"
                          + " queue(appointment_id,patient_id,doctor_id,work_date,queue_number,room_assigned,status)"
                          + " VALUES(?,8,6,CURDATE(),?,'P101','IN_CONSULTATION')",
                      appointment,
                      100000 + next));
          recordId =
              Actors.id(
                  Jdbc.insert(
                      c,
                      "INSERT INTO medical_record(appointment_id,patient_id,doctor_id,diagnosis)"
                          + " VALUES(?,8,6,'')",
                      appointment));
          return null;
        });
    new EMRService(doctor)
        .recordClinicalDiagnosis(
            queueId, new DiagnosisRequest("Triệu chứng thử", "J00", "Chẩn đoán thử", ""));
  }

  @AfterEach
  void cleanOwnFixture() {
    for (String token : filesToDelete) new AttachmentStorage().deleteOwn(token);
    if (queueId > 0)
      Database.tx(
          c -> {
            Jdbc.update(
                c,
                "UPDATE appointment a JOIN queue q ON q.appointment_id=a.appointment_id SET"
                    + " a.status='COMPLETED' WHERE q.queue_id=?",
                queueId);
            Jdbc.update(c, "UPDATE queue SET status='COMPLETED' WHERE queue_id=?", queueId);
            return null;
          });
  }

  private ClinicalOrder createOrder() {
    return new ClinicalService(doctor)
        .createClinicalOrders(new ClinicalOrderRequest(recordId, List.of(2), "Test"))
        .get(0);
  }

  private long createInvoice() {
    return new BillingService().createInvoice(cashier, recordId);
  }

  private TestResult approveResult(int order) {
    var data = new TestResult();
    data.setResultData("Kết quả gốc");
    data.setConclusion("Kết luận gốc");
    return new ClinicalService(lab).recordTestResult(order, data);
  }

  private UploadData signedReport() throws IOException {
    var out = new ByteArrayOutputStream();
    new ReportExportService()
        .exportPDF("Biên bản thử", List.of(Map.of("Lý do", "Sai thông tin thử nghiệm")), out);
    return new UploadData("signed-report.pdf", "application/pdf", out.toByteArray());
  }

  private void rememberProof(int requestId) {
    var request = Database.read(c -> new ResultRecallDAO().findById(c, requestId));
    if (request.getApprovalFileUrl() != null) filesToDelete.add(request.getApprovalFileUrl());
  }

  private long total(long invoice) {
    return Jdbc.number(
        Jdbc.read("SELECT total_amount FROM invoice WHERE invoice_id=?", invoice).get(0),
        "total_amount");
  }

  @Test
  void cancellationRemovesUnpaidChargeButCannotCancelPaidOrder() {
    var order = createOrder();
    long invoice = createInvoice();
    assertEquals(240000, total(invoice));
    assertTrue(new ClinicalService(doctor).cancelOrder(order.getOrderId()));
    assertEquals(150000, total(invoice));
    var second =
        new ClinicalService(doctor)
            .createClinicalOrders(new ClinicalOrderRequest(recordId, List.of(3), "Test"))
            .get(0);
    assertEquals(invoice, createInvoice());
    new BillingService().processCashPayment(cashier, invoice);
    assertThrows(
        BusinessException.class,
        () -> new ClinicalService(doctor).cancelOrder(second.getOrderId()));
    assertEquals(270000, total(invoice));
  }

  @Test
  void activeGatewayAttemptBlocksCancellationAndExpiredAttemptIsInvalidated() {
    var order = createOrder();
    long invoice = createInvoice();
    String reference = "REV" + UUID.randomUUID();
    Database.tx(
        c -> {
          Jdbc.update(
              c,
              "INSERT INTO payment_attempt(attempt_id,invoice_id,amount) VALUES(?,?,240000)",
              reference,
              invoice);
          return null;
        });
    assertThrows(
        BusinessException.class, () -> new ClinicalService(doctor).cancelOrder(order.getOrderId()));
    assertEquals(240000, total(invoice));
    assertEquals("PENDING", new ClinicalOrderDAO().findById(order.getOrderId()).getStatus());
    Database.tx(
        c -> {
          Jdbc.update(
              c,
              "UPDATE payment_attempt SET created_at=DATE_SUB(NOW(),INTERVAL 16 MINUTE) WHERE"
                  + " attempt_id=?",
              reference);
          return null;
        });
    assertTrue(new ClinicalService(doctor).cancelOrder(order.getOrderId()));
    assertEquals(150000, total(invoice));
    assertEquals(
        "EXPIRED",
        Jdbc.read("SELECT status FROM payment_attempt WHERE attempt_id=?", reference)
            .get(0)
            .get("status"));
  }

  @Test
  void paymentRacingCancellationKeepsInvoiceAndOrderConsistent() throws Exception {
    var order = createOrder();
    long invoice = createInvoice();
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      Future<?> cancel =
          executor.submit(
              () -> {
                start.await();
                try {
                  new ClinicalService(doctor).cancelOrder(order.getOrderId());
                } catch (BusinessException ignored) {
                }
                return null;
              });
      Future<?> pay =
          executor.submit(
              () -> {
                start.await();
                new BillingService().processCashPayment(cashier, invoice);
                return null;
              });
      start.countDown();
      cancel.get(15, TimeUnit.SECONDS);
      pay.get(15, TimeUnit.SECONDS);
      var saved = new ClinicalOrderDAO().findById(order.getOrderId());
      if ("CANCELLED".equals(saved.getStatus())) assertEquals(150000, total(invoice));
      else {
        assertEquals("PAID", saved.getPaymentStatus());
        assertEquals(240000, total(invoice));
      }
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void approvedRecallPreservesOriginalAndAllowsOneReplacement() throws Exception {
    var order = createOrder();
    long invoice = createInvoice();
    new BillingService().processCashPayment(cashier, invoice);
    var original = approveResult(order.getOrderId());
    int request =
        new ResultRecallService(lab)
            .requestRecall(original.getResultId(), "Cần đối chiếu lại kết quả");
    new ResultRecallService(head)
        .reviewRequest(request, true, "Đồng ý theo biên bản", signedReport());
    rememberProof(request);
    assertEquals(
        "RESULT_RECALLED", new ClinicalOrderDAO().findById(order.getOrderId()).getStatus());
    assertNull(new TestResultDAO().findByOrderId(order.getOrderId()));
    assertThrows(
        BusinessException.class,
        () -> new ClinicalService(doctor).getAttachment(original.getResultId()));
    var replacement = new TestResult();
    replacement.setResultData("Kết quả thay thế");
    var saved = new ClinicalService(lab).recordTestResult(order.getOrderId(), replacement);
    assertNotEquals(original.getResultId(), saved.getResultId());
    var archived = Database.read(c -> new TestResultDAO().findById(c, original.getResultId()));
    assertEquals("RECALLED", archived.getStatus());
    assertEquals("Kết quả gốc", archived.getResultData());
    assertEquals(
        "Kết quả thay thế", new TestResultDAO().findByOrderId(order.getOrderId()).getResultData());
    var worklist =
        Database.read(c -> new ClinicalOrderDAO().worklist(c, order.getOrderId().toString(), ""));
    assertEquals(
        1, worklist.size(), "Một chỉ định chỉ hiện một lần dù có nhiều bản kết quả lịch sử");
    assertEquals(
        saved.getResultId().longValue(), ((Number) worklist.get(0).get("result_id")).longValue());
    assertEquals(
        2,
        Jdbc.number(
            Jdbc.read("SELECT COUNT(*) n FROM test_result WHERE order_id=?", order.getOrderId())
                .get(0),
            "n"));
  }

  @Test
  void recallRequiresHeadOfCorrectDepartmentAndSignedPdf() throws Exception {
    var order = createOrder();
    long invoice = createInvoice();
    new BillingService().processCashPayment(cashier, invoice);
    var result = approveResult(order.getOrderId());
    int request =
        new ResultRecallService(lab).requestRecall(result.getResultId(), "Đề nghị kiểm tra");
    assertThrows(
        BusinessException.class,
        () -> new ResultRecallService(lab).requestRecall(result.getResultId(), "Trùng"));
    assertThrows(
        BusinessException.class,
        () ->
            new ResultRecallService(new AuthUser(2, "Manager", "MANAGER"))
                .reviewRequest(request, true, "Không đúng quyền", null));
    assertThrows(
        BusinessException.class,
        () -> new ResultRecallService(head).reviewRequest(request, true, "Thiếu biên bản", null));
    assertThrows(
        BusinessException.class,
        () ->
            new ResultRecallService(head)
                .reviewRequest(
                    request,
                    true,
                    "Sai định dạng",
                    new UploadData("fake.pdf", "application/pdf", "not PDF".getBytes())));
    long otherHead =
        Database.tx(
            c -> {
              long role =
                  Jdbc.number(
                      Jdbc.one(c, "SELECT role_id FROM roles WHERE name='DEPARTMENT_HEAD'"),
                      "role_id");
              long id =
                  Jdbc.insert(
                      c,
                      "INSERT INTO users(role_id,email,password,full_name,phone_number)"
                          + " VALUES(?,?,?,'Other head','0000000000')",
                      role,
                      "head-" + UUID.randomUUID() + "@test.local",
                      "unused");
              Jdbc.update(
                  c,
                  "INSERT INTO doctors(doctor_id,department_id,room_number) VALUES(?,2,'P201')",
                  id);
              return id;
            });
    assertThrows(
        BusinessException.class,
        () ->
            new ResultRecallService(new AuthUser(otherHead, "Other", "DEPARTMENT_HEAD"))
                .reviewRequest(request, true, "Khác khoa", signedReport()));
    new ResultRecallService(head).reviewRequest(request, false, "Chưa đủ căn cứ thu hồi", null);
    assertEquals("APPROVED", new TestResultDAO().findByOrderId(order.getOrderId()).getStatus());
    assertEquals("COMPLETED", new ClinicalOrderDAO().findById(order.getOrderId()).getStatus());
    assertEquals(
        "REJECTED", Database.read(c -> new ResultRecallDAO().findById(c, request)).getStatus());
  }

  @Test
  void twoRecallApprovalsOnlyApproveOnce() throws Exception {
    var order = createOrder();
    long invoice = createInvoice();
    new BillingService().processCashPayment(cashier, invoice);
    var result = approveResult(order.getOrderId());
    int request = new ResultRecallService(lab).requestRecall(result.getResultId(), "Kiểm tra lại");
    var executor = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    UploadData proof = signedReport();
    try {
      List<Future<Boolean>> attempts = new ArrayList<>();
      for (int n = 0; n < 2; n++)
        attempts.add(
            executor.submit(
                () -> {
                  gate.await();
                  try {
                    new ResultRecallService(head)
                        .reviewRequest(request, true, "Đã kiểm tra", proof);
                    return true;
                  } catch (BusinessException e) {
                    return false;
                  }
                }));
      gate.countDown();
      int success = 0;
      for (var future : attempts) if (future.get(15, TimeUnit.SECONDS)) success++;
      assertEquals(1, success);
      rememberProof(request);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void doctorNotificationsAreLimitedToTheirAssignedPatients() {
    var service = new ClinicalUpdateService(doctor);
    long cursor = service.latestId();
    var order = createOrder();
    long invoice = createInvoice();
    new BillingService().processCashPayment(cashier, invoice);
    approveResult(order.getOrderId());
    assertTrue(
        service.findAfter(cursor).stream()
            .anyMatch(update -> update.getOrderId().equals(order.getOrderId())));
    var other = new ClinicalUpdateService(new AuthUser(7, "Other", "DOCTOR"));
    assertTrue(
        other.findAfter(0).stream()
            .noneMatch(update -> update.getOrderId().equals(order.getOrderId())));
    assertThrows(BusinessException.class, () -> new ClinicalUpdateService(lab).findAfter(0));
  }

  @Test
  void dailySummaryCanBeRepeatedWithoutDuplicateRows() {
    var order = createOrder();
    long invoice = createInvoice();
    new BillingService().processCashPayment(cashier, invoice);
    LocalDate today = LocalDate.now();
    var summaries = new ReportSummaryDAO();
    summaries.saveDailySummary(today);
    var first = summaries.findByDate(today);
    summaries.saveDailySummary(today);
    var second = summaries.findByDate(today);
    assertEquals(first.get("appointment_count"), second.get("appointment_count"));
    assertEquals(first.get("total_revenue"), second.get("total_revenue"));
    var filter = new ReportFilter();
    filter.setFromDate(today);
    filter.setToDate(today);
    filter.setGroupBy("DAY");
    filter.setPaymentMethod("");
    var realtime =
        new AnalyticsService(new AuthUser(2, "Manager", "MANAGER")).getRevenueReport(filter);
    assertEquals(0, realtime.getTotalRevenue().compareTo((BigDecimal) second.get("total_revenue")));
    var batch = new ReportBatchService();
    batch.summarizePreviousDay();
    batch.summarizePreviousDay();
    assertNotNull(summaries.findByDate(today.minusDays(1)));
  }

  @Test
  void invoiceCreationRacingCancellationNeverChargesCancelledTest() throws Exception {
    var order = createOrder();
    var executor = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    try {
      var create =
          executor.submit(
              () -> {
                gate.await();
                return createInvoice();
              });
      var cancel =
          executor.submit(
              () -> {
                gate.await();
                return new ClinicalService(doctor).cancelOrder(order.getOrderId());
              });
      gate.countDown();
      long invoice = create.get(15, TimeUnit.SECONDS);
      assertTrue(cancel.get(15, TimeUnit.SECONDS));
      assertEquals(150000, total(invoice));
      assertTrue(
          Jdbc.read(
                  "SELECT line_id FROM invoice_line WHERE source_type='ORDER' AND source_id=?",
                  order.getOrderId())
              .isEmpty());
    } finally {
      executor.shutdownNow();
    }
  }
}
