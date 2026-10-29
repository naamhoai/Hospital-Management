package vn.hospital;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hospital.model.User;
import vn.hospital.util.FrontendResponse;

class FrontendResponseTest {
  @Test
  void serializesDatesAndRemovesSecretsFromMapsAndBeans() throws Exception {
    var user = new User();
    user.setPassword("never-expose-this-hash");
    var writer = new StringWriter();
    HttpServletResponse response = response(writer);
    FrontendResponse.write(
        response,
        Map.of(
            "user",
            user,
            "nested",
            List.of(
                Map.of(
                    "password_hash",
                    "secret",
                    "hashSecret",
                    "merchant-secret",
                    "date",
                    LocalDate.of(2026, 10, 29))),
            "csrf",
            "allowed-csrf-token"));
    String json = writer.toString();
    assertFalse(json.contains("never-expose"));
    assertFalse(json.contains("merchant-secret"));
    assertFalse(json.contains("password_hash"));
    assertTrue(json.contains("2026-10-29"));
    assertTrue(json.contains("allowed-csrf-token"));
  }

  @Test
  void shellUsesActualContextForNestedRoutesAndLoadsBundledAssets() throws Exception {
    var context =
        (ServletContext)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {ServletContext.class},
                (object, method, args) ->
                    method.getName().equals("getResourceAsStream")
                        ? new ByteArrayInputStream(
                            ("<meta content=\"__APP_CONTEXT__\"><script"
                                 + " src=\"./assets/react/app.js\"></script>")
                                .getBytes(StandardCharsets.UTF_8))
                        : null);
    var request =
        (HttpServletRequest)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (object, method, args) ->
                    switch (method.getName()) {
                      case "getContextPath" -> "/custom-hospital";
                      case "getServletContext" -> context;
                      default -> null;
                    });
    var writer = new StringWriter();
    FrontendResponse.shell(request, response(writer));
    assertTrue(writer.toString().contains("src=\"/custom-hospital/assets/react/app.js\""));
    assertFalse(writer.toString().contains("__APP_CONTEXT__"));
  }

  private HttpServletResponse response(StringWriter writer) {
    return (HttpServletResponse)
        Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] {HttpServletResponse.class},
            (object, method, args) ->
                method.getName().equals("getWriter") ? new PrintWriter(writer) : null);
  }
}
