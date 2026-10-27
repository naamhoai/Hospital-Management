package vn.hospital.integration;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import vn.hospital.util.*;

public class VNPayClient {
  public static String canonical(Map<String, String> data) {
    var fields = new TreeMap<String, String>(data);
    fields.remove("vnp_SecureHash");
    fields.remove("vnp_SecureHashType");
    return fields.entrySet().stream()
        .filter(
            e -> e.getKey().startsWith("vnp_") && e.getValue() != null && !e.getValue().isEmpty())
        .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
        .collect(java.util.stream.Collectors.joining("&"));
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.US_ASCII);
  }

  public static String sign(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA512");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
      return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean verifyCallback(Map<String, String> params) {
    String secret = Config.get("VNPAY_HASH_SECRET");
    if (secret.isBlank()) return false;
    String signature = params.getOrDefault("vnp_SecureHash", "");
    return MessageDigest.isEqual(
        sign(canonical(params), secret).getBytes(StandardCharsets.US_ASCII),
        signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
  }

  public String createPaymentUrl(String ref, BigDecimal amount, String ip) {
    Checks.require(
        !Config.get("VNPAY_TMN_CODE").isBlank() && !Config.get("VNPAY_HASH_SECRET").isBlank(),
        "Chưa cấu hình tài khoản VNPay sandbox. Có thể thanh toán tiền mặt.");
    var time = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    var fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    Map<String, String> data = new HashMap<>();
    data.put("vnp_Version", "2.1.0");
    data.put("vnp_Command", "pay");
    data.put("vnp_TmnCode", Config.get("VNPAY_TMN_CODE"));
    data.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
    data.put("vnp_CurrCode", "VND");
    data.put("vnp_TxnRef", ref);
    data.put("vnp_OrderInfo", "Hospital payment " + ref);
    data.put("vnp_OrderType", "other");
    data.put("vnp_Locale", "vn");
    data.put("vnp_ReturnUrl", Config.get("APP_BASE_URL") + "/payment/return");
    data.put("vnp_IpAddr", ip);
    data.put("vnp_CreateDate", time.format(fmt));
    data.put("vnp_ExpireDate", time.plusMinutes(15).format(fmt));
    String query = canonical(data);
    return Config.get("VNPAY_URL")
        + "?"
        + query
        + "&vnp_SecureHash="
        + sign(query, Config.get("VNPAY_HASH_SECRET"));
  }
}
