package vn.hospital.util;

import vn.hospital.model.AuthUser;

public final class Actors {
  private Actors() {}

  public static AuthUser require(AuthUser actor, String... roles) {
    Checks.require(actor != null && actor.is(roles), "Bạn không có quyền thực hiện chức năng này.");
    return actor;
  }

  public static int id(long value) {
    try {
      return Math.toIntExact(value);
    } catch (ArithmeticException e) {
      throw new BusinessException("Mã vượt giới hạn cho phép.");
    }
  }
}
