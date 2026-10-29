package vn.hospital.service;

import java.util.List;
import vn.hospital.dao.ClinicalUpdateDAO;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class ClinicalUpdateService {
  private final AuthUser actor;

  public ClinicalUpdateService(AuthUser actor) {
    this.actor = actor;
  }

  public long latestId() {
    Actors.require(actor, "DOCTOR");
    return Database.read(c -> new ClinicalUpdateDAO().latestId(c, actor.getUserId()));
  }

  public List<ClinicalUpdate> findAfter(long after) {
    Actors.require(actor, "DOCTOR");
    Checks.require(after >= 0, "Mốc thông báo không hợp lệ.");
    return Database.read(c -> new ClinicalUpdateDAO().findAfter(c, actor.getUserId(), after));
  }
}
