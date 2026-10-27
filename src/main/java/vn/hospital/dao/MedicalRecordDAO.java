package vn.hospital.dao;
import java.sql.*;
import java.util.*;
public class MedicalRecordDAO {
    public Map<String,Object> findByQueueId(Connection c,long queue) throws SQLException { return Jdbc.one(c,"SELECT * FROM medical_record WHERE queue_id=? FOR UPDATE",queue); }
    public Map<String,Object> findById(Connection c,long id) throws SQLException { return Jdbc.one(c,"SELECT * FROM medical_record WHERE record_id=? FOR UPDATE",id); }
    public void save(Connection c,long id,String symptoms,String code,String diagnosis,String notes) throws SQLException { Jdbc.update(c,"UPDATE medical_record SET symptoms=?,icd_code=?,diagnosis=?,notes=? WHERE record_id=?",symptoms,code,diagnosis,notes,id); }
}
