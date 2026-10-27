package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.service.QueueService;

@WebServlet("/queue")
public class QueueController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    r.setAttribute(
        "queue", new QueueService().findQueueForActor(user(r).getRole(), user(r).getUserId()));
    r.setAttribute("rooms", new QueueService().findActiveRooms());
    view(r, s, "queue");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    if (p(r, "action").equals("next")) {
      vn.hospital.util.Checks.require(user(r).is("DOCTOR"), "Chỉ bác sĩ được gọi bệnh nhân.");
      Long id = new QueueService().callNext(user(r));
      if (id != null) {
        s.sendRedirect(r.getContextPath() + "/emr?queueId=" + id);
        return;
      }
      redirect(r, s, "/queue", "Không có bệnh nhân chờ.");
    } else if (p(r, "action").equals("route")) {
      vn.hospital.util.Checks.require(
          user(r).is("ADMIN", "RECEPTIONIST"), "Chỉ lễ tân được điều phối.");
      new QueueService().routePatient(user(r), id(r, "queueId"), p(r, "room"));
      redirect(r, s, "/queue", "Đã chuyển phòng khám.");
    } else throw new vn.hospital.util.BusinessException("Thao tác không hợp lệ.");
  }
}
