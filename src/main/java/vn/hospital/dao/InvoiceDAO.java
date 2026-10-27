package vn.hospital.dao;
import java.sql.*;
import java.util.*;
public class InvoiceDAO {
    public Map<String,Object> findById(Connection c,long id) throws SQLException { return Jdbc.one(c,"SELECT * FROM invoice WHERE invoice_id=? FOR UPDATE",id); }
    public List<Map<String,Object>> list() { return Jdbc.read("SELECT i.*,u.full_name patient_name FROM invoice i JOIN users u ON u.user_id=i.patient_id ORDER BY invoice_id DESC"); }
}
