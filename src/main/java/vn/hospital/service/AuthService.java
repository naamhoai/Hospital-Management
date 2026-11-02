package vn.hospital.service;

import java.sql.Timestamp;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;
import vn.hospital.util.Rows;

public class AuthService {
  private final UserDAO users = new UserDAO();
  private static final String DUMMY = Passwords.hash("non-user-password");

  /** Xác minh tài khoản, đếm lần sai và chỉ tạo dữ liệu phiên sau khi mật khẩu hợp lệ. */
  public AuthUser login(String email, String password) {
    String login = Checks.text(email, 100, "Email").toLowerCase();
    AuthUser result =
        Database.tx(
            connection -> {
              var userRow = users.findByEmail(connection, login);
              if (userRow == null) {
                Passwords.matches(password, DUMMY);
                return null;
              }
              long id = Rows.number(userRow, "user_id");
              if (!"ACTIVE".equals(userRow.get("status"))
                  || (userRow.get("locked_until") instanceof Timestamp t
                      && t.toInstant().isAfter(java.time.Instant.now()))) return null;
              if (!Passwords.matches(password, (String) userRow.get("password_hash"))) {
                users.failed(connection, id);
                AuditDAO.record(connection, id, "LOGIN_FAILED", "users", id);
                return null;
              }
              users.clearFailures(connection, id);
              AuditDAO.record(connection, id, "LOGIN", "users", id);
              return new AuthUser(
                  id, (String) userRow.get("full_name"), (String) userRow.get("role"));
            });
    Checks.require(result != null, "Sai email/mật khẩu hoặc tài khoản đang tạm khóa.");
    return result;
  }
}
