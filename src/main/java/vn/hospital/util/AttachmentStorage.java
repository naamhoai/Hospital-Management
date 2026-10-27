package vn.hospital.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import vn.hospital.model.UploadData;

public class AttachmentStorage {
  public static final int MAX_BYTES = 5 * 1024 * 1024;

  private Path root() {
    String configured = Config.get("UPLOAD_DIRECTORY");
    Path root =
        Path.of(configured.isBlank() ? ".local/uploads" : configured).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create upload storage", e);
    }
    return root;
  }

  public String save(UploadData data) {
    byte[] b = data.bytes();
    Checks.require(
        b.length > 0 && b.length <= MAX_BYTES, "Tệp kết quả phải nhỏ hơn hoặc bằng 5 MB.");
    String suffix;
    if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-')
      suffix = ".pdf";
    else if (b.length >= 8
        && b[0] == (byte) 0x89
        && b[1] == 'P'
        && b[2] == 'N'
        && b[3] == 'G'
        && b[4] == 13
        && b[5] == 10
        && b[6] == 26
        && b[7] == 10) suffix = ".png";
    else if (b.length >= 3 && b[0] == (byte) 0xff && b[1] == (byte) 0xd8 && b[2] == (byte) 0xff)
      suffix = ".jpg";
    else
      throw new BusinessException(
          "Chỉ chấp nhận tệp PDF, PNG hoặc JPEG có nội dung đúng định dạng.");
    String name = UUID.randomUUID() + suffix;
    try {
      Files.write(root().resolve(name), b, StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot store result attachment", e);
    }
    return "attachment:" + name;
  }

  public File resolve(String token) {
    Checks.require(
        token != null && token.matches("attachment:[0-9a-f-]{36}\\.(pdf|png|jpg)"),
        "Tệp đính kèm không hợp lệ.");
    Path path = root().resolve(token.substring(11)).normalize();
    Checks.require(
        path.startsWith(root()) && Files.isRegularFile(path), "Không tìm thấy tệp kết quả.");
    return path.toFile();
  }

  public void deleteOwn(String token) {
    if (token == null || !token.startsWith("attachment:")) return;
    try {
      Files.deleteIfExists(resolve(token).toPath());
    } catch (IOException | BusinessException ignored) {
    }
  }
}
