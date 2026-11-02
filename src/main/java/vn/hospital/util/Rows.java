package vn.hospital.util;

import java.util.Map;

/** Đọc số từ dòng JOIN phục vụ màn hình; dữ liệu nghiệp vụ riêng dùng Java Bean. */
public final class Rows {
  private Rows() {}

  public static long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }
}
