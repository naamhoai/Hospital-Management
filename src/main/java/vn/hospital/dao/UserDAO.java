package vn.hospital.dao;
import java.sql.*;
import java.util.*;
public class UserDAO {
    public Map<String,Object> findByEmail(Connection c, String email) throws SQLException {
        return Jdbc.one(c,"SELECT u.*,r.name role FROM users u JOIN roles r USING(role_id) WHERE email=? FOR UPDATE",email);
    }
    public void failed(Connection c, long id) throws SQLException {
        Jdbc.update(c,"UPDATE users SET failed_attempts=failed_attempts+1,locked_until=CASE WHEN failed_attempts>=4 THEN DATE_ADD(NOW(),INTERVAL 15 MINUTE) ELSE locked_until END WHERE user_id=?",id);
    }
    public void clearFailures(Connection c, long id) throws SQLException { Jdbc.update(c,"UPDATE users SET failed_attempts=0,locked_until=NULL WHERE user_id=?",id); }
}
