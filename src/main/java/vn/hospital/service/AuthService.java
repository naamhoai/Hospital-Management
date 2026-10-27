package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;
import java.sql.Timestamp;
public class AuthService {
    private final UserDAO users = new UserDAO();
    private static final String DUMMY = Passwords.hash("non-user-password");
    public AuthUser login(String email, String password) {
        String login = Checks.text(email,100,"Email").toLowerCase();
        AuthUser result = Database.tx(c -> {
            var u=users.findByEmail(c,login);
            if (u==null) { Passwords.matches(password,DUMMY); return null; }
            long id=Jdbc.number(u,"user_id");
            if (!"ACTIVE".equals(u.get("status")) || (u.get("locked_until") instanceof Timestamp t && t.toInstant().isAfter(java.time.Instant.now()))) return null;
            if (!Passwords.matches(password,(String)u.get("password_hash"))) { users.failed(c,id); AuditDAO.record(c,id,"LOGIN_FAILED","users",id); return null; }
            users.clearFailures(c,id); AuditDAO.record(c,id,"LOGIN","users",id);
            return new AuthUser(id,(String)u.get("full_name"),(String)u.get("role"));
        });
        Checks.require(result!=null,"Sai email/mật khẩu hoặc tài khoản đang tạm khóa."); return result;
    }
}
