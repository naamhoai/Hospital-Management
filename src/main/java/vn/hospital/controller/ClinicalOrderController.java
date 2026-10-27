package vn.hospital.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.model.ClinicalOrderRequest;
import vn.hospital.service.ClinicalService;
import vn.hospital.util.*;

@WebServlet("/clinical-orders")
public class ClinicalOrderController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s) throws IOException {
    int order = Actors.id(id(r, "id"));
    download(
        s,
        new ClinicalService(user(r)).printClinicalOrder(order),
        "application/pdf",
        "clinical-order-" + order + ".pdf");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    var service = new ClinicalService(user(r));
    if (p(r, "action").equals("cancel")) service.cancelOrder(Actors.id(id(r, "orderId")));
    else {
      String[] ids = r.getParameterValues("serviceId");
      Checks.require(ids != null, "Chọn ít nhất một dịch vụ.");
      service.createClinicalOrders(
          new ClinicalOrderRequest(
              Actors.id(id(r, "recordId")),
              java.util.Arrays.stream(ids).map(v -> Actors.id(Checks.id(v))).toList(),
              p(r, "instructions")));
    }
    redirect(r, s, "/emr?queueId=" + id(r, "queueId"), "Đã cập nhật chỉ định.");
  }
}
