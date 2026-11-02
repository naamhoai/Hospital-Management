package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.service.QueueService;
import vn.hospital.util.BusinessException;
import vn.hospital.util.Checks;

@WebServlet("/queue")
public class QueueController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setAttribute(
        "queue",
        new QueueService().findQueueForActor(user(request).getRole(), user(request).getUserId()));
    request.setAttribute("rooms", new QueueService().findActiveRooms());
    view(request, response, "queue");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (param(request, "action").equals("next")) {
      Checks.require(user(request).is("DOCTOR"), "Chỉ bác sĩ được gọi bệnh nhân.");
      Long id = new QueueService().callNext(user(request));
      if (id != null) {
        response.sendRedirect(request.getContextPath() + "/emr?queueId=" + id);
        return;
      }
      redirect(request, response, "/queue", "Không có bệnh nhân chờ.");
    } else if (param(request, "action").equals("route")) {
      Checks.require(user(request).is("ADMIN", "RECEPTIONIST"), "Chỉ lễ tân được điều phối.");
      new QueueService()
          .routePatient(user(request), id(request, "queueId"), param(request, "room"));
      redirect(request, response, "/queue", "Đã chuyển phòng khám.");
    } else throw new BusinessException("Thao tác không hợp lệ.");
  }
}
