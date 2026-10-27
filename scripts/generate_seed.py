"""Regenerate synthetic demo data only; schema import is a separate, explicit step."""
from pathlib import Path
import hashlib, base64

root = Path(__file__).resolve().parents[1]
salt = b"group6-demo-seed!"
digest = hashlib.pbkdf2_hmac("sha256", b"Demo@1234", salt, 210000)
password_hash = "pbkdf2$210000$" + base64.b64encode(salt).decode() + "$" + base64.b64encode(digest).decode()
sql = """USE hospital;
INSERT INTO roles(role_id,name,description) VALUES
(1,'ADMIN','Quản trị'),(2,'MANAGER','Quản lý'),(3,'CASHIER','Thu ngân'),
(4,'RECEPTIONIST','Lễ tân'),(5,'LAB_TECHNICIAN','Kỹ thuật viên'),(6,'DOCTOR','Bác sĩ'),(7,'PATIENT','Bệnh nhân');
INSERT INTO users(user_id,role_id,email,password_hash,full_name,phone_number) VALUES
"""
accounts = [(1,1,"admin","Quản trị Demo"),(2,2,"manager","Quản lý Demo"),(3,3,"cashier","Thu ngân Demo"),(4,4,"reception","Lễ tân Demo"),(5,5,"lab","Kỹ thuật viên Demo"),(6,6,"doctor","Bác sĩ An Demo"),(7,6,"doctor2","Bác sĩ Bình Demo"),(8,7,"patient","Bệnh nhân Một Demo"),(9,7,"patient2","Bệnh nhân Hai Demo"),(10,7,"patient3","Bệnh nhân Ba Demo")]
sql += ",\n".join(f"({i},{role},'{email}@hospital.test','{password_hash}','{name}','090000{i:04d}')" for i,role,email,name in accounts)+";\n"
sql += """INSERT INTO departments VALUES(1,'Nội tổng quát','Khám tổng quát','ACTIVE'),(2,'Tai Mũi Họng','Chuyên khoa TMH','ACTIVE');
INSERT INTO rooms(department_id,room_number,room_name,room_type) VALUES(1,'P101','Phòng Nội 1','CONSULTATION'),(1,'P102','Phòng Nội 2','CONSULTATION'),(2,'P201','Phòng TMH','CONSULTATION'),(1,'LAB01','Phòng xét nghiệm','LAB');
INSERT INTO doctors(doctor_id,department_id,room_number,academic_title,experience_years) VALUES(6,1,'P101','Bác sĩ',8),(7,2,'P201','Thạc sĩ',6);
INSERT INTO patients(patient_id,date_of_birth,address) VALUES(8,'2000-01-01','Dữ liệu minh họa'),(9,'1995-02-02','Dữ liệu minh họa'),(10,'1990-03-03','Dữ liệu minh họa');
INSERT INTO service(service_id,department_id,name,category,unit_price) VALUES(1,1,'Khám chuyên khoa','CONSULTATION',150000),(2,1,'Xét nghiệm công thức máu','LAB',90000),(3,1,'X-quang ngực','IMAGING',120000),(4,2,'Nội soi tai mũi họng','IMAGING',200000);
INSERT INTO medication(name,active_ingredient,dosage_form,unit,unit_price,stock_quantity) VALUES('Thuốc mẫu A - dữ liệu demo','DEMO A','Viên','Viên',1000,500),('Thuốc mẫu B - dữ liệu demo','DEMO B','Viên','Viên',2500,300),('Thuốc mẫu C - dữ liệu demo','DEMO C','Chai','Chai',25000,100);
INSERT INTO icd_catalog VALUES('J00','Viêm mũi họng cấp'),('J06.9','Nhiễm khuẩn hô hấp trên cấp, không xác định'),('R50.9','Sốt, không xác định'),('R51','Đau đầu');
"""
for doctor in (6,7):
    for offset in (0,1,2,7,8):
        for shift,start,end in (("MORNING","08:00","12:00"),("AFTERNOON","13:00","17:00"),("EVENING","18:00","23:00")):
            sql += f"INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES({doctor},DATE_ADD(CURDATE(),INTERVAL {offset} DAY),'{shift}','{start}','{end}',10);\n"
(root/"database/02-seed.sql").write_text(sql,encoding="utf-8")
print("Generated database/02-seed.sql (synthetic demo accounts)")
