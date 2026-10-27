package vn.hospital.service;
import vn.hospital.dao.*;
import vn.hospital.model.AuthUser;
import vn.hospital.util.*;
import java.math.BigDecimal;
import java.util.Set;
public class CatalogService {
    public void save(AuthUser actor,long id,long department,String name,String category,BigDecimal price,String status) {
        String title=Checks.text(name,100,"Tên dịch vụ"); Checks.require(Set.of("CONSULTATION","LAB","IMAGING").contains(category) && Set.of("ACTIVE","INACTIVE").contains(status),"Loại hoặc trạng thái dịch vụ không hợp lệ.");
        Database.tx(c -> {
            Checks.require(Jdbc.one(c,"SELECT department_id FROM departments WHERE department_id=?",department)!=null,"Chuyên khoa không tồn tại.");
            long key=id;
            if(id==0) key=Jdbc.insert(c,"INSERT INTO service(department_id,name,category,unit_price,status) VALUES(?,?,?,?,?)",department,title,category,price,status);
            else Checks.require(Jdbc.update(c,"UPDATE service SET department_id=?,name=?,category=?,unit_price=?,status=? WHERE service_id=?",department,title,category,price,status,id)==1,"Dịch vụ không tồn tại.");
            AuditDAO.record(c,actor.getUserId(),"SAVE_SERVICE","service",key); return null;
        });
    }
}
