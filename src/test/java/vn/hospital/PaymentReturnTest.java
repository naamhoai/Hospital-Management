package vn.hospital;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hospital.controller.PaymentCallbackController;

class PaymentReturnTest {
  private static class Controller extends PaymentCallbackController {
    void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
      super.service(request, response);
    }
  }

  @Test
  void browserReturnLoadsReactWithoutConsumingTheCallback() throws Exception {
    var exchange = new Exchange(false, "/payment/return");
    new Controller().handle(exchange.request(), exchange.response());
    assertEquals(200, exchange.status);
    assertTrue(exchange.body.toString().contains("id=\"root\""));
    assertNull(exchange.attributes.get("error"));
  }

  @Test
  void apiReturnStillRejectsUnsignedCallback() throws Exception {
    var exchange = new Exchange(true, "/payment/return");
    new Controller().handle(exchange.request(), exchange.response());
    assertEquals(422, exchange.status);
    assertTrue(exchange.body.toString().contains("\"page\":\"error\""));
    assertNotNull(exchange.attributes.get("error"));
  }

  @Test
  void ipnRemainsAGatewayJsonEndpoint() throws Exception {
    var exchange = new Exchange(false, "/payment/ipn");
    new Controller().handle(exchange.request(), exchange.response());
    assertEquals(200, exchange.status);
    assertTrue(exchange.body.toString().contains("\"RspCode\":\"99\""));
    assertFalse(exchange.body.toString().contains("id=\"root\""));
  }

  private static class Exchange {
    final Map<String, Object> attributes = new HashMap<>();
    final StringWriter body = new StringWriter();
    final String path;
    int status = 200;

    Exchange(boolean api, String path) {
      attributes.put("hospital.api", api);
      this.path = path;
    }

    HttpServletRequest request() {
      ServletContext context =
          proxy(
              ServletContext.class,
              (object, method, args) -> {
                if (method.getName().equals("getResourceAsStream"))
                  return new ByteArrayInputStream(
                      "<div id=\"root\"></div>".getBytes(StandardCharsets.UTF_8));
                if (method.getName().equals("getAttribute")) return Map.of();
                return null;
              });
      return proxy(
          HttpServletRequest.class,
          (object, method, args) ->
              switch (method.getName()) {
                case "getMethod" -> "GET";
                case "getServletPath" -> path;
                case "getContextPath" -> "/hospital";
                case "getServletContext" -> context;
                case "getParameterMap" -> Map.of("vnp_ResponseCode", new String[] {"24"});
                case "getAttribute" -> attributes.get(args[0]);
                case "setAttribute" -> {
                  attributes.put((String) args[0], args[1]);
                  yield null;
                }
                case "getProtocol" -> "HTTP/1.1";
                default -> null;
              });
    }

    HttpServletResponse response() {
      return proxy(
          HttpServletResponse.class,
          (object, method, args) -> {
            if (method.getName().equals("getWriter")) return new PrintWriter(body);
            if (method.getName().equals("setStatus")) status = (Integer) args[0];
            return null;
          });
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }
}
