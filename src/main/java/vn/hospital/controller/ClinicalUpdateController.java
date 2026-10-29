package vn.hospital.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.ClinicalUpdateService;
import vn.hospital.util.*;

@WebServlet("/clinical-updates")
public class ClinicalUpdateController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    var service = new ClinicalUpdateService(user(request));
    response.setContentType("application/json");
    if (p(request, "afterId").isBlank()) {
      response.getWriter().write("{\"cursor\":" + service.latestId() + ",\"updates\":[]}");
      return;
    }
    long cursor;
    try {
      cursor = Long.parseLong(p(request, "afterId"));
    } catch (NumberFormatException error) {
      throw new BusinessException("Mốc thông báo không hợp lệ.");
    }
    var updates = service.findAfter(cursor);
    StringBuilder json = new StringBuilder("{\"updates\":[");
    for (int i = 0; i < updates.size(); i++) {
      var item = updates.get(i);
      if (i > 0) json.append(',');
      String message =
          item.getPatientName()
              + " · Chỉ định #"
              + item.getOrderId()
              + ("RECALL_RESULT".equals(item.getAction())
                  ? " đã thu hồi kết quả."
                  : " đã có kết quả mới.");
      json.append("{\"id\":")
          .append(item.getId())
          .append(",\"queueId\":")
          .append(item.getQueueId())
          .append(",\"message\":")
          .append(Json.quote(message))
          .append('}');
      cursor = item.getId();
    }
    json.append("],\"cursor\":").append(cursor).append('}');
    response.getWriter().write(json.toString());
  }
}
