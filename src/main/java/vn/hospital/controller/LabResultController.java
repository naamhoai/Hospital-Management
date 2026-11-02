package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.model.*;
import vn.hospital.service.ClinicalService;
import vn.hospital.util.*;

@WebServlet("/lab")
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 5500000, fileSizeThreshold = 0)
public class LabResultController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    new ClinicalService(user(request))
        .labWorkspace(
            param(request, "q"),
            param(request, "status"),
            param(request, "orderId").isBlank() ? null : Actors.id(id(request, "orderId")))
        .forEach(request::setAttribute);
    view(request, response, "lab");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    var result = new TestResult();
    result.setResultData(param(request, "resultData"));
    result.setConclusion(param(request, "conclusion"));
    result.setFileUrl(param(request, "fileUrl"));
    UploadData upload = upload(request, "attachment");
    new ClinicalService(user(request))
        .recordTestResult(Actors.id(id(request, "orderId")), result, upload);
    redirect(
        request,
        response,
        "/lab?orderId=" + id(request, "orderId"),
        "Đã duyệt kết quả. Kết quả được khóa để giữ toàn vẹn dữ liệu.");
  }
}
