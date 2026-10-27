package vn.hospital.service;
import vn.hospital.dao.ReportDAO;
import vn.hospital.util.*;
import java.time.*;
import java.util.*;
public class AnalyticsService {
    public List<Map<String,Object>> generateReport(String type,String from,String to,String method) {
        LocalDate start=Checks.date(from),end=Checks.date(to); Checks.require(!end.isBefore(start) && !end.isAfter(start.plusYears(2)),"Khoảng ngày phải hợp lệ và không quá 2 năm."); Checks.require(Set.of("","CASH","VNPAY").contains(method),"Phương thức không hợp lệ.");
        return switch(type) { case "flow" -> new ReportDAO().queryPatientFlowMetrics(start,end); case "revenue" -> new ReportDAO().queryRevenueMetrics(start,end,method); default -> throw new BusinessException("Loại báo cáo không hợp lệ."); };
    }
}
