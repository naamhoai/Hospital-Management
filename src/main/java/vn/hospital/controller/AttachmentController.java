package vn.hospital.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.Files;
import vn.hospital.service.ClinicalService;
import vn.hospital.util.Actors;

@WebServlet("/attachments")
public class AttachmentController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s) throws IOException {
    File file = new ClinicalService(user(r)).getAttachment(Actors.id(id(r, "resultId")));
    s.setContentType(Files.probeContentType(file.toPath()));
    s.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
    s.setContentLengthLong(file.length());
    Files.copy(file.toPath(), s.getOutputStream());
  }
}
