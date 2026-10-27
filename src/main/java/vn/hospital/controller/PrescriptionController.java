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
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    var service = new PrescriptionService(user(r));
    Integer id = Actors.id(id(r, "id"));
    if (p(r, "format").equals("pdf")) {
      download(s, service.printPrescription(id), "application/pdf", "prescription-" + id + ".pdf");
      return;
    }
    service.prescriptionPage(id).forEach(r::setAttribute);
    view(r, s, "prescription");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    Actors.require(user(r), "DOCTOR");
    String[] meds = r.getParameterValues("medicationId"),
        quantities = r.getParameterValues("quantity"),
        dosages = r.getParameterValues("dosage");
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
    var request = new PrescriptionRequest();
    request.setRecordId(Actors.id(id(r, "recordId")));
    request.setItems(items);
    request.setAdvice(p(r, "advice"));
    request.setOverrideInteractions("true".equals(p(r, "overrideInteractions")));
    var rx = new PrescriptionService(user(r)).issuePrescription(request);
    redirect(r, s, "/prescriptions?id=" + rx.getPrescriptionId(), "Đã tạo đơn thuốc.");
  }
}
