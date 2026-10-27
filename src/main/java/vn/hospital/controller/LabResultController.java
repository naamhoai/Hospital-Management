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
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    new ClinicalService(user(r))
        .labWorkspace(
            p(r, "q"),
            p(r, "status"),
            p(r, "orderId").isBlank() ? null : Actors.id(id(r, "orderId")))
        .forEach(r::setAttribute);
    view(r, s, "lab");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    var result = new TestResult();
    result.setResultData(p(r, "resultData"));
    result.setConclusion(p(r, "conclusion"));
    result.setFileUrl(p(r, "fileUrl"));
    Part part =
        r.getContentType() != null && r.getContentType().startsWith("multipart/")
            ? r.getPart("attachment")
            : null;
    UploadData upload =
        part == null || part.getSize() == 0
            ? null
            : new UploadData(
                part.getSubmittedFileName(),
                part.getContentType(),
                part.getInputStream().readAllBytes());
    new ClinicalService(user(r)).recordTestResult(Actors.id(id(r, "orderId")), result, upload);
    redirect(
        r,
        s,
        "/lab?orderId=" + id(r, "orderId"),
        "Đã duyệt kết quả. Kết quả được khóa để giữ toàn vẹn dữ liệu.");
  }
}
