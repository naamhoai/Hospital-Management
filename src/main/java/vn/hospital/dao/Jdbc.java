package vn.hospital.dao;

import java.sql.*;
import java.util.*;
import vn.hospital.util.Database;

public final class Jdbc {
  private Jdbc() {}

  private static PreparedStatement prepare(Connection c, String sql, Object... args)
      throws SQLException {
    PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
    return p;
  }

  public static List<Map<String, Object>> list(Connection c, String sql, Object... args)
      throws SQLException {
    try (var p = prepare(c, sql, args);
        var rs = p.executeQuery()) {
      List<Map<String, Object>> rows = new ArrayList<>();
      var m = rs.getMetaData();
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= m.getColumnCount(); i++) {
          Object value = rs.getObject(i);
          if (value instanceof java.time.LocalDateTime time) value = Timestamp.valueOf(time);
          row.put(m.getColumnLabel(i), value);
        }
        rows.add(row);
      }
      return rows;
    }
  }

  public static Map<String, Object> one(Connection c, String sql, Object... args)
      throws SQLException {
    var rows = list(c, sql, args);
    return rows.isEmpty() ? null : rows.get(0);
  }

  public static int update(Connection c, String sql, Object... args) throws SQLException {
    try (var p = prepare(c, sql, args)) {
      return p.executeUpdate();
    }
  }

  public static long insert(Connection c, String sql, Object... args) throws SQLException {
    try (var p = prepare(c, sql, args)) {
      p.executeUpdate();
      try (var rs = p.getGeneratedKeys()) {
        if (rs.next()) return rs.getLong(1);
        throw new SQLException("No generated key");
      }
    }
  }

  public static List<Map<String, Object>> read(String sql, Object... args) {
    return Database.tx(c -> list(c, sql, args));
  }

  public static long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }
}
