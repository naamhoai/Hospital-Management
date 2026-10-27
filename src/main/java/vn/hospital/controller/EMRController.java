package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.model.DiagnosisRequest;
import vn.hospital.service.EMRService;
import vn.hospital.util.*;

@WebServlet("/emr")
public class EMRController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    var page =
        new EMRService(user(r))
            .workspace(p(r, "queueId").isBlank() ? null : Actors.id(id(r, "queueId")), p(r, "q"));
    page.forEach(r::setAttribute);
    if (!p(r, "medicationKeyword").isBlank())
      r.setAttribute(
          "medications",
          new vn.hospital.service.PrescriptionService(user(r))
              .searchMedication(p(r, "medicationKeyword")));
    view(r, s, "emr");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    int queue = Actors.id(id(r, "queueId"));
    var service = new EMRService(user(r));
    try {
      switch (p(r, "action")) {
        case "close" -> service.closeRecord(queue);
        case "diagnosis" ->
            service.recordClinicalDiagnosis(
                queue,
                new DiagnosisRequest(
                    p(r, "symptoms"), p(r, "icdCode"), p(r, "diagnosis"), p(r, "notes")));
        default -> throw new BusinessException("Thao tác không hợp lệ.");
      }
      redirect(r, s, "/emr?queueId=" + queue, "Đã cập nhật bệnh án.");
    } catch (BusinessException e) {
      s.setStatus(422);
      r.setAttribute("error", e.getMessage());
      r.setAttribute("submitted", true);
      doGet(r, s);
    }
  }
}
