package vn.hospital.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class IcdCatalog {
  private IcdCatalog() {}

  public static List<Map<String, String>> entries() {
    try (InputStream in =
        Config.get("ICD_CATALOG_FILE").isBlank()
            ? IcdCatalog.class.getResourceAsStream("/icd-catalog.csv")
            : Files.newInputStream(Path.of(Config.get("ICD_CATALOG_FILE")))) {
      if (in == null) throw new IllegalStateException("ICD catalog not found");
      var entries = new ArrayList<Map<String, String>>();
      try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.isBlank() || line.startsWith("#")) continue;
          String[] cells = line.split(",", 2);
          if (cells.length == 2)
            entries.add(Map.of("code", cells[0].trim(), "description", cells[1].trim()));
        }
      }
      return entries;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load ICD catalog", e);
    }
  }

  public static boolean contains(String code) {
    return entries().stream().anyMatch(e -> e.get("code").equals(code));
  }
}
