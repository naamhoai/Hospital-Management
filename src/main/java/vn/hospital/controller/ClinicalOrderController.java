package vn.hospital.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.model.ClinicalOrderRequest;
import vn.hospital.service.ClinicalService;
import vn.hospital.util.*;

@WebServlet("/clinical-orders")
public class ClinicalOrderController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    int order = Actors.id(id(request, "id"));
    download(
        response,
        new ClinicalService(user(request)).printClinicalOrder(order),
        "application/pdf",
        "clinical-order-" + order + ".pdf");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    var service = new ClinicalService(user(request));
    if (param(request, "action").equals("cancel"))
      service.cancelOrder(Actors.id(id(request, "orderId")));
    else {
      String[] ids = request.getParameterValues("serviceId");
      Checks.require(ids != null, "Chọn ít nhất một dịch vụ.");
      service.createClinicalOrders(
          new ClinicalOrderRequest(
              Actors.id(id(request, "recordId")),
              java.util.Arrays.stream(ids).map(v -> Actors.id(Checks.id(v))).toList(),
              param(request, "instructions")));
    }
    redirect(request, response, "/emr?queueId=" + id(request, "queueId"), "Đã cập nhật chỉ định.");
  }
}
