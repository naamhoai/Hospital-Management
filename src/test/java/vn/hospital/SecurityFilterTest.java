package vn.hospital;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hospital.filter.SecurityFilter;
import vn.hospital.model.AuthUser;

class SecurityFilterTest {
  private static class Exchange {
    String method = "POST", path = "/login", location, forwarded;
    int status = 200, reached;
    String pathInfo;
    final StringWriter body = new StringWriter();
    boolean hasSession;
    final Map<String, Object> attributes = new HashMap<>(), requestAttributes = new HashMap<>();
    final Map<String, String> parameters = new HashMap<>();
    final HttpSession session =
        proxy(
            HttpSession.class,
            (p, m, a) ->
                switch (m.getName()) {
                  case "getAttribute" -> attributes.get(a[0]);
                  case "setAttribute" -> {
                    attributes.put((String) a[0], a[1]);
                    yield null;
                  }
                  default -> empty(m.getReturnType());
                });

    HttpServletRequest request() {
      return proxy(
          HttpServletRequest.class,
          (p, m, a) ->
              switch (m.getName()) {
                case "getMethod" -> method;
                case "getServletPath" -> path;
                case "getPathInfo" -> pathInfo;
                case "getAttribute" -> requestAttributes.get(a[0]);
                case "getServletContext" ->
                    proxy(
                        ServletContext.class,
                        (context, method, args) -> {
                          if (method.getName().equals("getResourceAsStream"))
                            return new ByteArrayInputStream("<div id=\"root\"></div>".getBytes());
                          if (method.getName().equals("getAttribute")) return Map.of();
                          return empty(method.getReturnType());
                        });
                case "getContextPath" -> "/hospital";
                case "getParameter" -> parameters.get(a[0]);
                case "getSession" -> {
                  boolean create = a == null || a.length == 0 || Boolean.TRUE.equals(a[0]);
                  if (create) hasSession = true;
                  yield hasSession ? session : null;
                }
                case "setAttribute" -> {
                  requestAttributes.put((String) a[0], a[1]);
                  yield null;
                }
                case "getRequestDispatcher" -> {
                  String destination = (String) a[0];
                  yield proxy(
                      RequestDispatcher.class,
                      (dispatcher, operation, args) -> {
                        if (operation.getName().equals("forward")) forwarded = destination;
                        return null;
                      });
                }
                default -> empty(m.getReturnType());
              });
    }

    HttpServletResponse response() {
      return proxy(
          HttpServletResponse.class,
          (p, m, a) ->
              switch (m.getName()) {
                case "setStatus", "sendError" -> {
                  status = (Integer) a[0];
                  yield null;
                }
                case "setHeader" -> {
                  if ("Location".equals(a[0])) location = (String) a[1];
                  yield null;
                }
                case "sendRedirect" -> {
                  status = 302;
                  location = (String) a[0];
                  yield null;
                }
                case "getWriter" -> new PrintWriter(body);
                default -> empty(m.getReturnType());
              });
    }

    void authenticated() {
      hasSession = true;
      attributes.put("csrf", "current-token");
      attributes.put("user", new AuthUser(8, "Demo patient", "PATIENT"));
    }

    void run() throws Exception {
      new SecurityFilter().doFilter(request(), response(), (r, s) -> reached++);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private static Object empty(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    return null;
  }

  @Test
  void staleLoginRedirectsToFreshGetWithoutRunningLogin() throws Exception {
    var e = new Exchange();
    e.parameters.put("csrf", "old-token");
    e.run();
    assertEquals(303, e.status);
    assertEquals("/hospital/login?reason=session-expired", e.location);
    assertEquals(0, e.reached);
    assertFalse(e.hasSession);
  }

  @Test
  void apiRoleCheckUsesTargetRouteInsteadOfApiPrefix() throws Exception {
    var e = new Exchange();
    e.authenticated();
    e.method = "GET";
    e.path = "/api";
    e.pathInfo = "/reports";
    e.run();
    assertEquals(403, e.status);
    assertEquals(0, e.reached);
    assertTrue(e.body.toString().contains("\"page\":\"error\""));
  }

  @Test
  void apiExpiredSessionReturnsJsonWithoutRunningPost() throws Exception {
    var e = new Exchange();
    e.path = "/api";
    e.pathInfo = "/appointments";
    e.parameters.put("csrf", "stale");
    e.run();
    assertEquals(401, e.status);
    assertEquals(0, e.reached);
    assertTrue(e.body.toString().contains("\"user\":null"));
  }

  @Test
  void apiStillRequiresCsrfForAuthenticatedMutation() throws Exception {
    var e = new Exchange();
    e.authenticated();
    e.path = "/api";
    e.pathInfo = "/appointments";
    e.parameters.put("csrf", "stale");
    e.run();
    assertEquals(403, e.status);
    assertEquals(0, e.reached);
  }

  @Test
  void expiredProtectedFormDoesNotPerformMutation() throws Exception {
    var e = new Exchange();
    e.path = "/appointments";
    e.parameters.put("csrf", "old-token");
    e.run();
    assertEquals(303, e.status);
    assertEquals("/hospital/login?reason=session-expired", e.location);
    assertEquals(0, e.reached);
  }

  @Test
  void invalidTokenWhileLoggedInKeeps403AndShowsAppError() throws Exception {
    var e = new Exchange();
    e.authenticated();
    e.path = "/appointments";
    e.parameters.put("csrf", "old-token");
    e.run();
    assertEquals(403, e.status);
    assertTrue(e.body.toString().contains("id=\"root\""));
    assertEquals(0, e.reached);
    assertEquals("current-token", e.attributes.get("csrf"));
  }

  @Test
  void validTokenAndRoleStillPass() throws Exception {
    var e = new Exchange();
    e.authenticated();
    e.path = "/appointments";
    e.parameters.put("csrf", "current-token");
    e.run();
    assertEquals(200, e.status);
    assertEquals(1, e.reached);
  }

  @Test
  void validCsrfDoesNotBypassRoleCheck() throws Exception {
    var e = new Exchange();
    e.authenticated();
    e.path = "/reports";
    e.parameters.put("csrf", "current-token");
    e.run();
    assertEquals(403, e.status);
    assertEquals(0, e.reached);
  }

  @Test
  void freshLoginGetCreatesToken() throws Exception {
    var e = new Exchange();
    e.method = "GET";
    e.run();
    assertEquals(1, e.reached);
    assertTrue(e.hasSession);
    assertNotNull(e.attributes.get("csrf"));
  }

  @Test
  void staleLoginInAnotherTabPreservesCurrentAuthentication() throws Exception {
    var e = new Exchange();
    e.authenticated();
    e.parameters.put("csrf", "old-token");
    e.run();
    assertEquals(303, e.status);
    assertEquals("/hospital/dashboard", e.location);
    assertNotNull(e.attributes.get("user"));
    assertEquals(0, e.reached);
  }
}
