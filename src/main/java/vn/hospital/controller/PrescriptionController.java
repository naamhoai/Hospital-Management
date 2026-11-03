package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import vn.hospital.model.*;
import vn.hospital.service.PrescriptionService;
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
      throws IOException {
    Actors.require(user(request), "DOCTOR");
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
}
