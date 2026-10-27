package vn.hospital.util;

import java.math.BigDecimal;
import java.time.*;

public final class Checks {
  private Checks() {}

  public static void require(boolean condition, String message) {
    if (!condition) throw new BusinessException(message);
  }

  public static String text(String value, int max, String field) {
    String s = value == null ? "" : value.trim();
    require(
        !s.isEmpty() && s.length() <= max,
        field + " không được trống và tối đa " + max + " ký tự.");
    return s;
  }

  public static long id(String value) {
    try {
      long id = Long.parseLong(value);
      require(id > 0, "Mã không hợp lệ.");
      return id;
    } catch (NumberFormatException e) {
      throw new BusinessException("Mã không hợp lệ.");
    }
  }

  public static BigDecimal money(String value) {
    try {
      BigDecimal n = new BigDecimal(value);
      require(
          n.signum() >= 0 && n.scale() <= 2 && n.compareTo(new BigDecimal("9999999999")) <= 0,
          "Số tiền không hợp lệ.");
      return n;
    } catch (NumberFormatException | NullPointerException e) {
      throw new BusinessException("Số tiền không hợp lệ.");
    }
  }

  public static LocalDate date(String s) {
    try {
      return LocalDate.parse(s);
    } catch (Exception e) {
      throw new BusinessException("Ngày không hợp lệ.");
    }
  }
}
