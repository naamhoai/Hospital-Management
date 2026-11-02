package vn.hospital.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.Files;
import vn.hospital.service.ClinicalService;
import vn.hospital.util.Actors;

@WebServlet("/attachments")
public class AttachmentController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    File file =
        new ClinicalService(user(request)).getAttachment(Actors.id(id(request, "resultId")));
    response.setContentType(Files.probeContentType(file.toPath()));
    response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
    response.setContentLengthLong(file.length());
    Files.copy(file.toPath(), response.getOutputStream());
  }
}
