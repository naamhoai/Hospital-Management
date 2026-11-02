package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.model.DiagnosisRequest;
import vn.hospital.service.EMRService;
import vn.hospital.service.PrescriptionService;
import vn.hospital.util.*;

@WebServlet("/emr")
public class EMRController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    var page =
        new EMRService(user(request))
            .workspace(
                param(request, "queueId").isBlank() ? null : Actors.id(id(request, "queueId")),
                param(request, "q"));
    page.forEach(request::setAttribute);
    if (!param(request, "medicationKeyword").isBlank())
      request.setAttribute(
          "medications",
          new PrescriptionService(user(request))
              .searchMedication(param(request, "medicationKeyword")));
    view(request, response, "emr");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    int queue = Actors.id(id(request, "queueId"));
    var service = new EMRService(user(request));
    try {
      switch (param(request, "action")) {
        case "close" -> service.closeRecord(queue);
        case "diagnosis" ->
            service.recordClinicalDiagnosis(
                queue,
                new DiagnosisRequest(
                    param(request, "symptoms"),
                    param(request, "icdCode"),
                    param(request, "diagnosis"),
                    param(request, "notes")));
        default -> throw new BusinessException("Thao tác không hợp lệ.");
      }
      redirect(request, response, "/emr?queueId=" + queue, "Đã cập nhật bệnh án.");
    } catch (BusinessException e) {
      response.setStatus(422);
      request.setAttribute("error", e.getMessage());
      request.setAttribute("submitted", true);
      doGet(request, response);
    }
  }
}
