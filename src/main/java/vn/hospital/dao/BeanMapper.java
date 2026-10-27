package vn.hospital.dao;

import java.beans.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Database rows are mapped here, never in Servlet controllers or JSPs. */
public final class BeanMapper {
  private BeanMapper() {}

  public static <T> T map(Map<String, Object> row, Class<T> type) {
    if (row == null) return null;
    try {
      T bean = type.getDeclaredConstructor().newInstance();
      for (PropertyDescriptor property : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
        Method setter = property.getWriteMethod();
        if (setter == null) continue;
        String column =
            property.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        if (row.containsKey(column))
          setter.invoke(bean, convert(row.get(column), property.getPropertyType()));
      }
      return bean;
    } catch (ReflectiveOperationException | IntrospectionException e) {
      throw new IllegalStateException("Cannot map " + type.getSimpleName(), e);
    }
  }

  private static Object convert(Object value, Class<?> type) {
    if (value == null || type.isInstance(value)) return value;
    if (value instanceof Number n) {
      if (type == Integer.class) return Math.toIntExact(n.longValue());
      if (type == Long.class) return n.longValue();
      if (type == BigDecimal.class) return new BigDecimal(n.toString());
    }
    if (type == LocalDate.class && value instanceof java.sql.Date d) return d.toLocalDate();
    if (type == LocalTime.class && value instanceof Time t) return t.toLocalTime();
    if (type == LocalDateTime.class && value instanceof Timestamp t) return t.toLocalDateTime();
    return value;
  }

  public static <T> List<T> list(List<Map<String, Object>> rows, Class<T> type) {
    return rows.stream().map(r -> map(r, type)).toList();
  }
}
