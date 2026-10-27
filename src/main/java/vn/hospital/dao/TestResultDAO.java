package vn.hospital.dao;

import java.sql.*;
import vn.hospital.model.TestResult;
import vn.hospital.util.*;

public class TestResultDAO {
  public TestResult findByOrderId(Integer id) {
    return Database.tx(c -> findByOrderId(c, id));
  }

  public TestResult findByOrderId(Connection c, long id) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(c, "SELECT * FROM test_result WHERE order_id=?", id), TestResult.class);
  }

  public TestResult findById(Connection c, long id) throws SQLException {
    return BeanMapper.map(
        Jdbc.one(c, "SELECT * FROM test_result WHERE result_id=?", id), TestResult.class);
  }

  public TestResult save(TestResult result) {
    return Database.tx(c -> save(c, result));
  }

  public TestResult save(Connection c, TestResult result) throws SQLException {
    result.setResultId(
        Actors.id(
            Jdbc.insert(
                c,
                "INSERT INTO test_result(order_id,technician_id,result_data,conclusion,file_url)"
                    + " VALUES(?,?,?,?,?)",
                result.getOrderId(),
                result.getTechnicianId(),
                result.getResultData(),
                result.getConclusion(),
                result.getFileUrl())));
    return findByOrderId(c, result.getOrderId());
  }
}
