package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.ResultRecallService;
import vn.hospital.util.*;

@WebServlet("/result-recalls")
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 5500000, fileSizeThreshold = 0)
public class ResultRecallController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    var service = new ResultRecallService(user(request));
    if (!p(request, "originalRequestId").isBlank()) {
      var file = service.getOriginalResultFile(Actors.id(id(request, "originalRequestId")));
      sendStoredFile(response, file, "original-result" + extension(file));
      return;
    }
    if (!p(request, "fileRequestId").isBlank()) {
      sendStoredFile(
          response,
          service.getApprovalFile(Actors.id(id(request, "fileRequestId"))),
          "recall-approval.pdf");
      return;
    }
    request.setAttribute("requests", service.listRequests());
    view(request, response, "result-recalls");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    var service = new ResultRecallService(user(request));
    String action = p(request, "action");
    if ("request".equals(action)) {
      service.requestRecall(Actors.id(id(request, "resultId")), p(request, "reason"));
      String path = user(request).is("DOCTOR") ? "/emr" : "/lab?orderId=" + id(request, "orderId");
      redirect(request, response, path, "Đã gửi đề nghị thu hồi kết quả cho trưởng khoa.");
      return;
    }
    Checks.require("approve".equals(action) || "reject".equals(action), "Thao tác không hợp lệ.");
    service.reviewRequest(
        Actors.id(id(request, "requestId")),
        "approve".equals(action),
        p(request, "reviewNote"),
        upload(request, "signedReport"));
    redirect(
        request,
        response,
        "/result-recalls",
        "Đã xử lý đề nghị. Lịch sử và bản kết quả cũ được giữ lại.");
  }

  private String extension(java.io.File file) {
    return file.getName().substring(file.getName().lastIndexOf('.'));
  }

  private void sendStoredFile(HttpServletResponse response, java.io.File file, String name)
      throws IOException {
    // Tệp gốc/biên bản là dữ liệu lưu lâu dài: tải xuống không được xóa file trên máy chủ.
    response.setContentType(java.nio.file.Files.probeContentType(file.toPath()));
    response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
    response.setContentLengthLong(file.length());
    java.nio.file.Files.copy(file.toPath(), response.getOutputStream());
  }
}
