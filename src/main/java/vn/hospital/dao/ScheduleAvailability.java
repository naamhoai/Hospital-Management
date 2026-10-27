package vn.hospital.dao;

import java.sql.*;
import java.time.*;
import java.util.*;

/** Counts future free appointment times, rather than merely unused shift capacity. */
public final class ScheduleAvailability {
  private ScheduleAvailability() {}

  public static List<Map<String, Object>> filter(Connection c, List<Map<String, Object>> schedules)
      throws SQLException {
    var result = new ArrayList<Map<String, Object>>();
    for (var w : schedules) {
      LocalDate day = ((java.sql.Date) w.get("work_date")).toLocalDate();
      LocalTime start = ((Time) w.get("start_time")).toLocalTime(),
          end = ((Time) w.get("end_time")).toLocalTime();
      long minutes = Duration.between(start, end).toMinutes() / Jdbc.number(w, "max_slots");
      if (minutes <= 0) continue;
      Set<LocalTime> taken = new HashSet<>();
      for (var a :
          Jdbc.list(
              c,
              "SELECT appointment_time FROM appointment WHERE schedule_id=? AND status IN"
                  + " ('BOOKED','CHECKED_IN','COMPLETED')",
              w.get("schedule_id"))) taken.add(((Time) a.get("appointment_time")).toLocalTime());
      int count = 0;
      for (int i = 0; i < Jdbc.number(w, "max_slots"); i++) {
        LocalTime t = start.plusMinutes(i * minutes);
        if (LocalDateTime.of(day, t).isAfter(LocalDateTime.now()) && !taken.contains(t)) count++;
      }
      if (count > 0) {
        w.put("remaining", count);
        result.add(w);
      }
    }
    return result;
  }
}
