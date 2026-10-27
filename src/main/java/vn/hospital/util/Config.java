package vn.hospital.util;

import java.util.Properties;

public final class Config {
  private static final Properties VALUES = new Properties();

  static {
    for (String file : new String[] {"application.properties", "application-local.properties"}) {
      try (var in = Config.class.getClassLoader().getResourceAsStream(file)) {
        if (in != null) VALUES.load(in);
      } catch (Exception e) {
        throw new IllegalStateException("Cannot load configuration", e);
      }
    }
  }

  private Config() {}

  public static String get(String key) {
    return System.getenv().getOrDefault(key, VALUES.getProperty(key, ""));
  }
}
