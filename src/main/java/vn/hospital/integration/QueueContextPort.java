package vn.hospital.integration;

import java.sql.*;
import vn.hospital.dao.QueueDAO;
import vn.hospital.model.QueueContext;
import vn.hospital.util.Database;

/** Read-only boundary between operations and clinical EMR. */
public class QueueContextPort {
  public QueueContext getContext(Integer queueId) {
    return Database.tx(c -> getContext(c, queueId));
  }

  public QueueContext getContext(Connection c, long queueId) throws SQLException {
    return new QueueDAO().findContext(c, queueId);
  }

  public boolean verifyQueueContext(Integer queueId) {
    return getContext(queueId) != null;
  }
}
