package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import vn.hospital.model.*;
import vn.hospital.service.PrescriptionService;
import vn.hospital.service.EMRService;
import vn.hospital.util.*;

@WebServlet("/prescriptions")
public class PrescriptionController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    var service = new PrescriptionService(user(request));
    Integer id = Actors.id(id(request, "id"));
    if (param(request, "format").equals("pdf")) {
      download(
          response,
          service.printPrescription(id),
          "application/pdf",
          "prescription-" + id + ".pdf");
      return;
    }
    service.prescriptionPage(id).forEach(request::setAttribute);
    view(request, response, "prescription");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Actors.require(user(request), "DOCTOR");
    try {
      issue(request, response);
    } catch (BusinessException error) {
      // Lỗi nhập đơn: mở lại đúng bệnh án, giữ các dòng thuốc để bác sĩ sửa, không trừ kho.
      if (param(request, "queueId").isBlank()) throw error;
      var page = new EMRService(user(request)).workspace(Actors.id(id(request, "queueId")), "");
      var record = (MedicalRecord) page.get("record");
      Checks.require(record.getRecordId().longValue() == id(request, "recordId"),
          "Đơn thuốc không thuộc lượt khám đang mở.");
      page.forEach(request::setAttribute);
      request.setAttribute("error", error.getMessage());
      request.setAttribute("prescriptionSubmitted", true);
      request.setAttribute("prescriptionRows", submittedRows(request));
      response.setStatus(422);
      view(request, response, "emr");
    }
  }

  /** SQ17: đọc các dòng từ JSP rồi chuyển cho Service kiểm tra và lưu trong transaction. */
  private void issue(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String[] meds = request.getParameterValues("medicationId"),
        quantities = request.getParameterValues("quantity"),
        dosages = request.getParameterValues("dosage");
    Checks.require(
        meds != null
            && quantities != null
            && dosages != null
            && meds.length == quantities.length
            && meds.length == dosages.length,
        "Dòng thuốc không hợp lệ.");
    var items = new ArrayList<PrescriptionDetail>();
    for (int i = 0; i < meds.length; i++) {
      if (meds[i].isBlank()) continue;
      var d = new PrescriptionDetail();
      d.setMedicationId(Actors.id(Checks.id(meds[i])));
      d.setQuantity(Actors.id(Checks.id(quantities[i])));
      d.setDosage(dosages[i]);
      items.add(d);
    }
    var prescriptionRequest = new PrescriptionRequest();
    prescriptionRequest.setRecordId(Actors.id(id(request, "recordId")));
    prescriptionRequest.setItems(items);
    prescriptionRequest.setAdvice(param(request, "advice"));
    prescriptionRequest.setOverrideInteractions(
        "true".equals(param(request, "overrideInteractions")));
    var rx = new PrescriptionService(user(request)).issuePrescription(prescriptionRequest);
    redirect(request, response, "/prescriptions?id=" + rx.getPrescriptionId(), "Đã tạo đơn thuốc.");
  }

  /** Giữ nguyên dữ liệu nhập sai để hiển thị lại; đây không phải dữ liệu đã lưu xuống database. */
  private List<Map<String, String>> submittedRows(HttpServletRequest request) {
    String[] meds = request.getParameterValues("medicationId"),
        quantities = request.getParameterValues("quantity"),
        dosages = request.getParameterValues("dosage");
    var rows = new ArrayList<Map<String, String>>();
    for (int i = 0; meds != null && i < Math.min(meds.length, 30); i++) {
      rows.add(Map.of("medicationId", meds[i],
          "quantity", quantities != null && i < quantities.length ? quantities[i] : "",
          "dosage", dosages != null && i < dosages.length ? dosages[i] : ""));
    }
    return rows.isEmpty() ? List.of(Map.of("medicationId", "", "quantity", "", "dosage", "")) : rows;
  }
}
