package vn.hospital;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import vn.hospital.dao.*;
import vn.hospital.model.*;
import vn.hospital.service.*;
import vn.hospital.util.*;

class DesignContractTest {
  @Test
  void controllersAndBusinessLogicDoNotExecuteSql() throws Exception {
    for (String layer : List.of("controller", "service", "integration", "listener")) {
      try (var files = Files.list(Path.of("src/main/java/vn/hospital", layer))) {
        for (Path file : files.toList()) {
          String source = Files.readString(file);
          assertFalse(
              source.matches(
                  "(?s).*\\b(?:SELECT|INSERT|UPDATE|DELETE|ALTER|CREATE)\\s+(?:INTO|FROM|TABLE|[a-z_]+"
                      + " SET).*"),
              file.toString());
          assertFalse(source.contains("Jdbc."), file.toString());
          if (layer.equals("controller"))
            assertFalse(source.contains("vn.hospital.dao"), file.toString());
        }
      }
    }
  }

  @Test
  void baselineKeepsEighteenSdsBusinessTables() throws Exception {
    String schema = Files.readString(Path.of("database/01-schema.sql"));
    assertEquals(18, schema.split("CREATE TABLE ").length - 1);
    assertFalse(schema.contains("password_hash"));
    assertFalse(schema.contains("FOREIGN KEY(queue_id)"));
    assertTrue(schema.contains("diagnosis VARCHAR(255) NOT NULL"));
  }

  @Test
  void emrAndAnalyticsUseTypedDesignContracts() throws Exception {
    assertEquals(
        MedicalRecord.class,
        EMRService.class.getMethod("getRecord", Integer.class).getReturnType());
    assertEquals(
        Prescription.class,
        PrescriptionService.class
            .getMethod("issuePrescription", PrescriptionRequest.class)
            .getReturnType());
    assertEquals(
        TestResult.class,
        ClinicalService.class
            .getMethod("recordTestResult", Integer.class, TestResult.class)
            .getReturnType());
    assertEquals(
        ReportData.class,
        AnalyticsService.class
            .getMethod("generateReport", String.class, ReportFilter.class)
            .getReturnType());
    assertEquals(
        File.class,
        AnalyticsService.class
            .getMethod("exportReport", String.class, ReportFilter.class, String.class)
            .getReturnType());
    assertEquals(ReportData.class, RevenueReport.class.getSuperclass());
  }

  @Test
  void optionalValuesAndDatabaseTypesMapToBeans() {
    Map<String, Object> row = new HashMap<>();
    row.put("record_id", 7L);
    row.put("patient_id", 8L);
    row.put("symptoms", null);
    row.put("diagnosis", "Đau đầu");
    var record = BeanMapper.map(row, MedicalRecord.class);
    assertEquals(7, record.getRecordId());
    assertNull(record.getSymptoms());
    assertEquals("Đau đầu", record.getDiagnosis());
  }

  @Test
  void unicodePdfKeepsVietnameseText() throws Exception {
    var out = new ByteArrayOutputStream();
    new ReportExportService()
        .exportPDF(
            "Báo cáo khám bệnh",
            List.of(Map.of("Chuyên khoa", "Nội tổng quát", "Kết luận", "Đã hoàn tất")),
            out);
    try (var pdf = PDDocument.load(out.toByteArray())) {
      String text = new PDFTextStripper().getText(pdf);
      assertTrue(text.contains("Báo cáo khám bệnh"));
      assertTrue(text.contains("Nội tổng quát"));
      assertTrue(text.contains("Đã hoàn tất"));
    }
  }

  @Test
  void reportFilterRejectsInvalidRangesAndRolesBeforeQuery() {
    var f = new ReportFilter();
    f.setFromDate(LocalDate.of(2026, 1, 1));
    f.setToDate(LocalDate.of(2025, 1, 1));
    var manager = new AnalyticsService(new AuthUser(2, "Manager", "MANAGER"));
    assertThrows(BusinessException.class, () -> manager.generateReport("flow", f));
    f.setToDate(LocalDate.of(2026, 2, 1));
    f.setGroupBy("DAY; DROP TABLE users");
    assertThrows(BusinessException.class, () -> manager.generateReport("flow", f));
    f.setGroupBy("DAY");
    assertThrows(
        BusinessException.class,
        () ->
            new AnalyticsService(new AuthUser(8, "Patient", "PATIENT"))
                .generateReport("revenue", f));
    assertThrows(
        BusinessException.class,
        () -> new AnalyticsService().generateReport("flow", "2026-01-01", "2026-01-02", ""));
  }

  @Test
  void rejectsExecutableUploadsAndTraversal() {
    var store = new AttachmentStorage();
    assertThrows(
        BusinessException.class,
        () ->
            store.save(
                new UploadData(
                    "report.pdf", "application/pdf", "<script>alert(1)</script>".getBytes())));
    assertThrows(BusinessException.class, () -> store.resolve("attachment:../secrets"));
    assertThrows(
        BusinessException.class,
        () ->
            store.save(
                new UploadData(
                    "large.pdf", "application/pdf", new byte[AttachmentStorage.MAX_BYTES + 1])));
  }
}
