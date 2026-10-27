package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.ClinicalService;
import vn.hospital.util.*;
@WebServlet("/clinical-orders")
public class ClinicalOrderController extends BaseController {
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        String[] ids=r.getParameterValues("serviceId"); Checks.require(ids!=null,"Chọn ít nhất một dịch vụ."); long[] services=java.util.Arrays.stream(ids).mapToLong(Checks::id).toArray();
        new ClinicalService().createClinicalOrders(user(r),id(r,"recordId"),services,p(r,"instructions")); redirect(r,s,"/emr?queueId="+id(r,"queueId"),"Đã tạo chỉ định. Cần thanh toán trước xét nghiệm.");
    }
}
