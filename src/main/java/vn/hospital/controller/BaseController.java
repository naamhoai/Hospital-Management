package vn.hospital.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import vn.hospital.model.AuthUser;
import vn.hospital.model.UploadData;
import vn.hospital.util.BusinessException;
import vn.hospital.util.Checks;

/** Các thao tác HTTP dùng chung; nghiệp vụ và SQL nằm ở Service/DAO. */
public abstract class BaseController extends HttpServlet {
  protected AuthUser user(HttpServletRequest request) {
    return (AuthUser) request.getSession().getAttribute("user");
  }

  protected String param(HttpServletRequest request, String name) {
    String value = request.getParameter(name);
    return value == null ? "" : value.trim();
  }

  protected long id(HttpServletRequest request, String name) {
    return Checks.id(param(request, name));
  }

  protected void view(HttpServletRequest request, HttpServletResponse response, String name)
      throws ServletException, IOException {
    request.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(request, response);
  }

  protected void redirect(
      HttpServletRequest request, HttpServletResponse response, String path, String message)
      throws IOException {
    request.getSession().setAttribute("flash", message);
    response.sendRedirect(request.getContextPath() + path);
  }

  /** Chỉ xóa file xuất tạm sau khi gửi; không dùng hàm này cho tệp kết quả lưu lâu dài. */
  protected void download(HttpServletResponse response, File file, String type, String name)
      throws IOException {
    try {
      response.setContentType(type);
      response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
      response.setContentLengthLong(file.length());
      Files.copy(file.toPath(), response.getOutputStream());
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      super.service(request, response);
    } catch (BusinessException error) {
      response.setStatus(422);
      request.setAttribute("error", error.getMessage());
      view(request, response, "error");
    } catch (RuntimeException error) {
      getServletContext().log("Request failed: " + request.getServletPath(), error);
      response.setStatus(500);
      request.setAttribute(
          "error", "Không xử lý được yêu cầu. Kiểm tra cấu hình database hoặc xem log Tomcat.");
      view(request, response, "error");
    }
  }

  protected UploadData upload(HttpServletRequest request, String field)
      throws ServletException, IOException {
    String contentType = request.getContentType();
    if (contentType == null || !contentType.startsWith("multipart/")) return null;
    Part file = request.getPart(field);
    if (file == null || file.getSize() == 0) return null;
    return new UploadData(
        file.getSubmittedFileName(), file.getContentType(), file.getInputStream().readAllBytes());
  }
}
