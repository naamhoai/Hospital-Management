package vn.hospital.util;

import com.google.gson.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/** Hợp đồng JSON chung giữa các Servlet hiện có và giao diện React. */
public final class FrontendResponse {
  private static final Gson GSON =
      new GsonBuilder()
          .serializeNulls()
          .registerTypeHierarchyAdapter(
              TemporalAccessor.class,
              (JsonSerializer<TemporalAccessor>)
                  (value, type, context) -> new JsonPrimitive(value.toString()))
          .registerTypeHierarchyAdapter(
              java.util.Date.class,
              (JsonSerializer<java.util.Date>)
                  (value, type, context) -> new JsonPrimitive(value.toString()))
          .create();

  // Chỉ gửi dữ liệu màn hình; không đưa các attribute nội bộ của Servlet vào API.
  private static final Set<String> PAGE_FIELDS =
      Set.of(
          "appointments",
          "schedules",
          "doctors",
          "departments",
          "services",
          "rooms",
          "queue",
          "patients",
          "records",
          "record",
          "patient",
          "doctorName",
          "queueContext",
          "clinicalProgress",
          "history",
          "icds",
          "medications",
          "hasPendingTests",
          "pharmacyConfigured",
          "orders",
          "prescriptions",
          "prescription",
          "selectedOrder",
          "recallHistory",
          "requests",
          "invoices",
          "invoice",
          "lines",
          "report",
          "rows",
          "filter",
          "type",
          "message");

  private FrontendResponse() {}

  public static boolean isApi(HttpServletRequest request) {
    return Boolean.TRUE.equals(request.getAttribute("hospital.api"));
  }

  public static void page(HttpServletRequest request, HttpServletResponse response, String name)
      throws IOException {
    Map<String, Object> data = new LinkedHashMap<>();
    for (String key : PAGE_FIELDS) {
      if (request.getAttribute(key) != null) data.put(key, request.getAttribute(key));
    }
    Map<String, Object> result = envelope(request);
    result.put("page", name);
    result.put("data", data);
    result.put("error", request.getAttribute("error"));
    var session = request.getSession(false);
    result.put("flash", session == null ? null : session.getAttribute("flash"));
    if (session != null) session.removeAttribute("flash");
    write(response, result);
  }

  public static void redirect(HttpServletRequest request, HttpServletResponse response, String path)
      throws IOException {
    Map<String, Object> result = envelope(request);
    result.put("redirect", path);
    write(response, result);
  }

  public static void error(
      HttpServletRequest request, HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    request.setAttribute("error", message);
    page(request, response, "error");
  }

  private static Map<String, Object> envelope(HttpServletRequest request) {
    var session = request.getSession(false);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("user", session == null ? null : session.getAttribute("user"));
    result.put("csrf", session == null ? null : session.getAttribute("csrf"));
    result.put("labels", request.getServletContext().getAttribute("labels"));
    return result;
  }

  public static void write(HttpServletResponse response, Object value) throws IOException {
    JsonElement json = GSON.toJsonTree(value);
    removeSecrets(json);
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json");
    response.getWriter().write(GSON.toJson(json));
  }

  /** Bảo vệ cả Java Bean lẫn các Map từ truy vấn JOIN, kể cả khi DAO bổ sung trường sau này. */
  private static void removeSecrets(JsonElement value) {
    if (value.isJsonArray()) {
      for (JsonElement item : value.getAsJsonArray()) removeSecrets(item);
    } else if (value.isJsonObject()) {
      JsonObject object = value.getAsJsonObject();
      for (String key : new ArrayList<>(object.keySet())) {
        String name = key.toLowerCase(Locale.ROOT).replace("_", "");
        if (Set.of("password", "passwordhash", "hashsecret", "apikey", "checksumkey")
            .contains(name)) object.remove(key);
        else removeSecrets(object.get(key));
      }
    }
  }

  public static void shell(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try (var input =
        request.getServletContext().getResourceAsStream("/WEB-INF/frontend/index.html")) {
      if (input == null)
        throw new IOException("React build is missing. Run npm ci and npm run build in frontend/.");
      String html =
          new String(input.readAllBytes(), StandardCharsets.UTF_8)
              .replace("__APP_CONTEXT__", request.getContextPath())
              .replace("\"./assets/", "\"" + request.getContextPath() + "/assets/");
      response.setContentType("text/html;charset=UTF-8");
      response.getWriter().write(html);
    }
  }
}
