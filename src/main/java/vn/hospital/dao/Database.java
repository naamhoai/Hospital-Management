package vn.hospital.dao;
import java.sql.*;
import vn.hospital.util.Config;
public final class Database {
    static { try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException e) { throw new ExceptionInInitializerError(e); } }
    private Database() {}
    public static Connection open() throws SQLException { Connection c=DriverManager.getConnection(Config.get("DB_URL"), Config.get("DB_USER"), Config.get("DB_PASSWORD")); try(var p=c.createStatement()) { p.execute("SET time_zone='+07:00'"); } catch(SQLException e) { c.close(); throw e; } return c; }
    @FunctionalInterface public interface Work<T> { T run(Connection c) throws Exception; }
    public static <T> T tx(Work<T> work) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try { T result = work.run(c); c.commit(); return result; }
            catch (Exception e) { c.rollback(); if (e instanceof RuntimeException r) throw r; throw new IllegalStateException("Database operation failed", e); }
        } catch (SQLException e) { throw new IllegalStateException("Cannot connect to database", e); }
    }
}
