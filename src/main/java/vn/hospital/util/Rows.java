package vn.hospital.util;

import java.util.Map;

/** Numeric access for joined screen projections; domain methods return Java Beans. */
public final class Rows {
  private Rows() {}

  public static long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }
}
