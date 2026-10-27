USE hospital;
INSERT INTO roles(role_id,name,description) VALUES
(1,'ADMIN','Quản trị'),(2,'MANAGER','Quản lý'),(3,'CASHIER','Thu ngân'),
(4,'RECEPTIONIST','Lễ tân'),(5,'LAB_TECHNICIAN','Kỹ thuật viên'),(6,'DOCTOR','Bác sĩ'),(7,'PATIENT','Bệnh nhân');
INSERT INTO users(user_id,role_id,email,password,full_name,phone_number) VALUES
(1,1,'admin@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Quản trị Demo','0900000001'),
(2,2,'manager@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Quản lý Demo','0900000002'),
(3,3,'cashier@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Thu ngân Demo','0900000003'),
(4,4,'reception@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Lễ tân Demo','0900000004'),
(5,5,'lab@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Kỹ thuật viên Demo','0900000005'),
(6,6,'doctor@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Bác sĩ An Demo','0900000006'),
(7,6,'doctor2@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Bác sĩ Bình Demo','0900000007'),
(8,7,'patient@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Bệnh nhân Một Demo','0900000008'),
(9,7,'patient2@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Bệnh nhân Hai Demo','0900000009'),
(10,7,'patient3@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Bệnh nhân Ba Demo','0900000010');
INSERT INTO departments VALUES(1,'Nội tổng quát','Khám tổng quát','ACTIVE'),(2,'Tai Mũi Họng','Chuyên khoa TMH','ACTIVE');
INSERT INTO rooms(department_id,room_number,room_name,room_type) VALUES(1,'P101','Phòng Nội 1','CONSULTATION'),(1,'P102','Phòng Nội 2','CONSULTATION'),(2,'P201','Phòng TMH','CONSULTATION'),(1,'LAB01','Phòng xét nghiệm','LAB');
INSERT INTO doctors(doctor_id,department_id,room_number,academic_title,experience_years) VALUES(6,1,'P101','Bác sĩ',8),(7,2,'P201','Thạc sĩ',6);
INSERT INTO patients(patient_id,date_of_birth,address) VALUES(8,'2000-01-01','Dữ liệu minh họa'),(9,'1995-02-02','Dữ liệu minh họa'),(10,'1990-03-03','Dữ liệu minh họa');
INSERT INTO service(service_id,department_id,name,category,unit_price) VALUES(1,1,'Khám chuyên khoa','CONSULTATION',150000),(2,1,'Xét nghiệm công thức máu','LAB',90000),(3,1,'X-quang ngực','IMAGING',120000),(4,2,'Nội soi tai mũi họng','IMAGING',200000);
INSERT INTO medication(name,active_ingredient,dosage_form,unit,unit_price,stock_quantity) VALUES('Thuốc mẫu A - dữ liệu demo','DEMO A','Viên','Viên',1000,500),('Thuốc mẫu B - dữ liệu demo','DEMO B','Viên','Viên',2500,300),('Thuốc mẫu C - dữ liệu demo','DEMO C','Chai','Chai',25000,100);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 8 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 8 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,DATE_ADD(CURDATE(),INTERVAL 8 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'EVENING','18:00','23:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 8 DAY),'MORNING','08:00','12:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 8 DAY),'AFTERNOON','13:00','17:00',10);
INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(7,DATE_ADD(CURDATE(),INTERVAL 8 DAY),'EVENING','18:00','23:00',10);
