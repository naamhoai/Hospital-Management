package vn.hospital.integration;

import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import vn.hospital.model.PrescriptionDetail;
import vn.hospital.util.*;

/** Optional external pharmacy adapter. An unconfigured provider is never reported as synced. */
public class PharmacyClient {
  public boolean isConfigured() {
    return !Config.get("PHARMACY_BASE_URL").isBlank();
  }

  public List<String> checkInteractions(List<PrescriptionDetail> items) {
    Checks.require(isConfigured(), "Chưa cấu hình hệ thống dược để kiểm tra tương tác tự động.");
    StringJoiner lines = new StringJoiner("\n");
    for (var i : items) lines.add(i.getMedicationId() + "," + i.getQuantity());
    return send("/interactions", lines.toString()).lines().filter(s -> !s.isBlank()).toList();
  }

  public Boolean syncPrescription(Integer id) {
    if (!isConfigured()) return false;
    send("/prescriptions", id.toString());
    return true;
  }

  private String send(String path, String body) {
    try {
      URI uri = URI.create(Config.get("PHARMACY_BASE_URL") + path);
      Checks.require("https".equals(uri.getScheme()), "Kết nối dược phải dùng HTTPS.");
      var request =
          HttpRequest.newBuilder(uri)
              .timeout(Duration.ofSeconds(8))
              .header("Content-Type", "text/plain; charset=UTF-8")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      var response =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(5))
              .build()
              .send(request, HttpResponse.BodyHandlers.ofString());
      Checks.require(
          response.statusCode() >= 200 && response.statusCode() < 300,
          "Hệ thống dược chưa xác nhận yêu cầu.");
      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BusinessException("Kết nối dược bị gián đoạn.");
    } catch (java.io.IOException | IllegalArgumentException e) {
      throw new BusinessException("Không kết nối được hệ thống dược.");
    }
  }
}
