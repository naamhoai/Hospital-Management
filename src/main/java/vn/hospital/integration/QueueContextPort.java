package vn.hospital.integration;
import vn.hospital.dao.Jdbc;
import java.sql.*;
import java.util.*;
public class QueueContextPort {
    public Map<String,Object> getContext(Connection c,long queue) throws SQLException { return Jdbc.one(c,"SELECT * FROM queue WHERE queue_id=?",queue); }
}
