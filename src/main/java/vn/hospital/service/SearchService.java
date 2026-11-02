package vn.hospital.service;

import java.util.*;
import vn.hospital.dao.SearchDAO;
import vn.hospital.util.*;

public class SearchService {
  /**
   * SQ04 - Tra cứu trước khi đặt lịch. Đầu vào: từ khóa tên bác sĩ/chuyên khoa/dịch vụ và ngày tùy
   * chọn. Kiểm tra bộ lọc rồi gọi SearchDAO; chỉ chọn ca còn giờ hẹn trong tương lai. Đầu ra: Map
   * doctors/services cho search.jsp; không giữ chỗ hoặc sửa database.
   */
  public Map<String, Object> searchDoctorsAndServices(String keyword, String date) {
    Checks.require(keyword.length() <= 100, "Từ khóa tối đa 100 ký tự.");
    if (!date.isBlank()) Checks.date(date);
    var dao = new SearchDAO();
    return Map.of("doctors", dao.doctors(keyword, date), "services", dao.services(keyword));
  }
}
