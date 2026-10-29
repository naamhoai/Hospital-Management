package vn.hospital;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.hospital.model.*;
import vn.hospital.util.Json;

class ClinicalProgressTest {
  @Test
  void progressFollowsDiagnosisTestsAndClose() {
    var record = new MedicalRecord();
    record.setStatus("OPEN");
    assertEquals(ClinicalProgress.NOT_DIAGNOSED, ClinicalProgress.from(record, List.of()));
    record.setDiagnosis("Chẩn đoán");
    assertEquals(ClinicalProgress.EXAMINED, ClinicalProgress.from(record, List.of()));
    var order = new ClinicalOrder();
    order.setStatus("PENDING");
    assertEquals(ClinicalProgress.WAITING_TESTS, ClinicalProgress.from(record, List.of(order)));
    order.setStatus("COMPLETED");
    assertEquals(ClinicalProgress.RESULTS_READY, ClinicalProgress.from(record, List.of(order)));
    record.setStatus("CLOSED");
    assertEquals(ClinicalProgress.FINISHED, ClinicalProgress.from(record, List.of(order)));
    order.setStatus("RESULT_RECALLED");
    assertEquals(ClinicalProgress.WAITING_TESTS, ClinicalProgress.from(record, List.of(order)));
  }

  @Test
  void cancelledTestsDoNotBlockProgress() {
    var record = new MedicalRecord();
    record.setStatus("OPEN");
    record.setDiagnosis("Đã chẩn đoán");
    var order = new ClinicalOrder();
    order.setStatus("CANCELLED");
    assertEquals(ClinicalProgress.EXAMINED, ClinicalProgress.from(record, List.of(order)));
  }

  @Test
  void jsonEscapesUserControlledText() {
    assertEquals("\"A\\\"B\\nC\\\\D\"", Json.quote("A\"B\nC\\D"));
    assertEquals("\"\\u0001\"", Json.quote("\u0001"));
    assertEquals("null", Json.quote(null));
  }
}
