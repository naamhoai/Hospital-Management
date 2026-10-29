package vn.hospital.util;

/** Escape chuỗi JSON trước khi đưa tên bệnh nhân/nội dung thông báo vào response. */
public final class Json {
  private Json() {}

  public static String quote(String text) {
    if (text == null) return "null";
    StringBuilder result = new StringBuilder("\"");
    for (char c : text.toCharArray()) {
      switch (c) {
        case '"' -> result.append("\\\"");
        case '\\' -> result.append("\\\\");
        case '\n' -> result.append("\\n");
        case '\r' -> result.append("\\r");
        case '\t' -> result.append("\\t");
        default -> {
          if (c < 32) result.append(String.format("\\u%04x", (int) c));
          else result.append(c);
        }
      }
    }
    return result.append('"').toString();
  }
}
