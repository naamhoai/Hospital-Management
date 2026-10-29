package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;

public abstract class BaseController extends HttpServlet {
  protected AuthUser user(HttpServletRequest r) {
    return (AuthUser) r.getSession().getAttribute("user");
  }

  protected String p(HttpServletRequest r, String key) {
    String s = r.getParameter(key);
    return s == null ? "" : s.trim();
  }

  protected long id(HttpServletRequest r, String key) {
    return Checks.id(p(r, key));
  }

  protected void view(HttpServletRequest r, HttpServletResponse s, String name)
      throws ServletException, IOException {
    if (FrontendResponse.isApi(r)) FrontendResponse.page(r, s, name);
    else FrontendResponse.shell(r, s);
  }

  protected void redirect(HttpServletRequest r, HttpServletResponse s, String path, String message)
      throws IOException {
    r.getSession().setAttribute("flash", message);
    s.sendRedirect(r.getContextPath() + path);
  }

  protected void download(HttpServletResponse s, java.io.File file, String type, String name)
      throws IOException {
    try {
      s.setContentType(type);
      s.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
      s.setContentLengthLong(file.length());
      java.nio.file.Files.copy(file.toPath(), s.getOutputStream());
    } finally {
      java.nio.file.Files.deleteIfExists(file.toPath());
    }
  }

  @Override
  protected void service(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    try {
      super.service(r, s);
    } catch (BusinessException e) {
      s.setStatus(422);
      r.setAttribute("error", e.getMessage());
      view(r, s, "error");
    } catch (RuntimeException e) {
      getServletContext().log("Request failed: " + r.getServletPath(), e);
      s.setStatus(500);
      r.setAttribute(
          "error", "Không xử lý được yêu cầu. Kiểm tra cấu hình database hoặc xem log Tomcat.");
      view(r, s, "error");
    }
  }

  protected vn.hospital.model.UploadData upload(HttpServletRequest request, String field)
      throws ServletException, IOException {
    if (request.getContentType() == null || !request.getContentType().startsWith("multipart/"))
      return null;
    Part file = request.getPart(field);
    if (file == null || file.getSize() == 0) return null;
    return new vn.hospital.model.UploadData(
        file.getSubmittedFileName(), file.getContentType(), file.getInputStream().readAllBytes());
  }
}
