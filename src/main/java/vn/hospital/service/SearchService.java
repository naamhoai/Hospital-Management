package vn.hospital.service;

import java.util.*;
import vn.hospital.dao.SearchDAO;
import vn.hospital.util.*;

public class SearchService {
  /** Tìm bác sĩ, ca còn giờ và dịch vụ; kiểm tra bộ lọc trước khi gửi tới DAO. */
  public Map<String, Object> searchDoctorsAndServices(String keyword, String date) {
    Checks.require(keyword.length() <= 100, "Từ khóa tối đa 100 ký tự.");
    if (!date.isBlank()) Checks.date(date);
    var dao = new SearchDAO();
    return Map.of("doctors", dao.doctors(keyword, date), "services", dao.services(keyword));
  }
}
