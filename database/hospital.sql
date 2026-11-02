-- HOSPITAL MANAGEMENT: MỘT FILE DUY NHẤT GỒM TẠO BẢNG VÀ DỮ LIỆU DEMO.
-- MySQL 8.4 trở lên. Workbench: File > Open SQL Script > Query > Execute (All or Selection).
-- Dùng database hospital_demo mới. Không có DROP, procedure, DELIMITER hoặc file seed phụ.
-- Nếu database đã có bảng: không chạy lại; giữ dữ liệu và tiếp tục demo trên web.
-- 18 bảng nghiệp vụ theo ERD và 8 bảng hỗ trợ cho triển khai, dòng phí, gói khám, nhật ký.
SET NAMES utf8mb4;
SET SESSION time_zone = '+07:00';

CREATE DATABASE IF NOT EXISTS hospital_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_demo;
SET NAMES utf8mb4;

-- Vai trò và tài khoản
CREATE TABLE roles (
    role_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    gender VARCHAR(10),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    FOREIGN KEY(role_id) REFERENCES roles(role_id)
);

-- Chuyên khoa, phòng và nhân sự
CREATE TABLE departments (
    department_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE rooms (
    room_id INT PRIMARY KEY AUTO_INCREMENT,
    department_id INT NOT NULL,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_name VARCHAR(100) NOT NULL,
    room_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY(department_id) REFERENCES departments(department_id)
);

CREATE TABLE doctors (
    doctor_id INT PRIMARY KEY,
    department_id INT NOT NULL,
    room_number VARCHAR(20) NULL,
    academic_title VARCHAR(50),
    experience_years INT DEFAULT 0,
    rating_average DECIMAL(3,2) DEFAULT 0,
    FOREIGN KEY(doctor_id) REFERENCES users(user_id),
    FOREIGN KEY(department_id) REFERENCES departments(department_id),
    FOREIGN KEY(room_number) REFERENCES rooms(room_number)
);

CREATE TABLE patients (
    patient_id INT PRIMARY KEY,
    identity_card VARCHAR(20) UNIQUE,
    insurance_number VARCHAR(30),
    date_of_birth DATE,
    address VARCHAR(255),
    emergency_contact VARCHAR(20),
    booking_blocked_until DATE NULL,
    FOREIGN KEY(patient_id) REFERENCES users(user_id)
);

-- Lịch bác sĩ, lịch hẹn và tiếp nhận
CREATE TABLE work_schedule (
    schedule_id INT PRIMARY KEY AUTO_INCREMENT,
    doctor_id INT NOT NULL,
    work_date DATE NOT NULL,
    shift VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_slots INT NOT NULL,
    booked_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    leave_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    leave_reason VARCHAR(255),
    leave_requested_at DATETIME,
    leave_reviewed_by INT,
    leave_reviewed_at DATETIME,
    leave_review_note VARCHAR(255),
    FOREIGN KEY(leave_reviewed_by) REFERENCES users(user_id),
    UNIQUE(doctor_id,work_date,shift),
    CHECK(max_slots>0 AND booked_count>=0 AND booked_count<=max_slots),
    CHECK(end_time>start_time),
    FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id)
);

CREATE TABLE appointment (
    appointment_id INT PRIMARY KEY AUTO_INCREMENT,
    appointment_code VARCHAR(50) NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    schedule_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    initial_symptoms VARCHAR(255),
    reschedule_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consultation_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    cancelled_at DATETIME NULL,
    active_slot DATETIME GENERATED ALWAYS AS (CASE WHEN status IN ('BOOKED','CHECKED_IN','COMPLETED') THEN TIMESTAMP(appointment_date,appointment_time) ELSE NULL END) STORED,
    FOREIGN KEY(patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id),
    FOREIGN KEY(schedule_id) REFERENCES work_schedule(schedule_id),
    INDEX(patient_id,status),
    UNIQUE KEY uq_doctor_active_slot(doctor_id,active_slot)
);

CREATE TABLE queue (
    queue_id INT PRIMARY KEY AUTO_INCREMENT,
    appointment_id INT NULL UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NULL,
    queue_number INT NOT NULL,
    room_assigned VARCHAR(20) NULL,
    priority_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    work_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(appointment_id) REFERENCES appointment(appointment_id),
    FOREIGN KEY(patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id),
    FOREIGN KEY(room_assigned) REFERENCES rooms(room_number),
    UNIQUE KEY uq_queue_number(work_date,room_assigned,queue_number)
);

-- Hồ sơ khám bệnh và cận lâm sàng
CREATE TABLE medical_record (
    record_id INT PRIMARY KEY AUTO_INCREMENT,
    appointment_id INT NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    symptoms TEXT,
    icd_code VARCHAR(20),
    diagnosis VARCHAR(255) NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at DATETIME NULL,
    FOREIGN KEY(appointment_id) REFERENCES appointment(appointment_id),
    FOREIGN KEY(patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id)
);

CREATE TABLE service (
    service_id INT PRIMARY KEY AUTO_INCREMENT,
    department_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CHECK(unit_price>=0),
    FOREIGN KEY(department_id) REFERENCES departments(department_id)
);

CREATE TABLE clinical_order (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    record_id INT NOT NULL,
    service_id INT NOT NULL,
    package_service_id INT,
    FOREIGN KEY(package_service_id) REFERENCES service(service_id),
    order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    instructions VARCHAR(255),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    UNIQUE(record_id,service_id),
    FOREIGN KEY(record_id) REFERENCES medical_record(record_id),
    FOREIGN KEY(service_id) REFERENCES service(service_id)
);

CREATE TABLE test_result (
    result_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    technician_id INT NOT NULL,
    result_data TEXT NOT NULL,
    conclusion VARCHAR(255),
    file_url VARCHAR(255),
    approved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    active_order_id INT GENERATED ALWAYS AS (CASE WHEN status='APPROVED' THEN order_id ELSE NULL END) STORED,
    FOREIGN KEY(order_id) REFERENCES clinical_order(order_id),
    FOREIGN KEY(technician_id) REFERENCES users(user_id),
    INDEX idx_result_order_history(order_id),
    UNIQUE KEY uq_active_order_result(active_order_id)
);

-- Thuốc và đơn thuốc
CREATE TABLE medication (
    medication_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    active_ingredient VARCHAR(100),
    dosage_form VARCHAR(50),
    unit VARCHAR(20) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CHECK(stock_quantity>=0 AND unit_price>=0)
);

CREATE TABLE prescription (
    prescription_id INT PRIMARY KEY AUTO_INCREMENT,
    record_id INT NOT NULL,
    prescribed_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    advice VARCHAR(255),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    FOREIGN KEY(record_id) REFERENCES medical_record(record_id)
);

CREATE TABLE prescription_detail (
    id INT PRIMARY KEY AUTO_INCREMENT,
    prescription_id INT NOT NULL,
    medication_id INT NOT NULL,
    quantity INT NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    CHECK(quantity>0),
    FOREIGN KEY(prescription_id) REFERENCES prescription(prescription_id),
    FOREIGN KEY(medication_id) REFERENCES medication(medication_id)
);

-- Hóa đơn và phản hồi
CREATE TABLE invoice (
    invoice_id INT PRIMARY KEY AUTO_INCREMENT,
    invoice_code VARCHAR(50) NOT NULL UNIQUE,
    record_id INT NOT NULL,
    patient_id INT NOT NULL,
    cashier_id INT,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'UNSELECTED',
    transaction_id VARCHAR(100) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    FOREIGN KEY(record_id) REFERENCES medical_record(record_id),
    FOREIGN KEY(patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY(cashier_id) REFERENCES users(user_id)
);

CREATE TABLE feedback (
    feedback_id INT PRIMARY KEY AUTO_INCREMENT,
    record_id INT NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK(rating BETWEEN 1 AND 5),
    FOREIGN KEY(record_id) REFERENCES medical_record(record_id),
    FOREIGN KEY(patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id)
);

-- Bảng hỗ trợ: số thứ tự, dòng phí, giao dịch, thông báo và nhật ký
CREATE TABLE queue_counter (
    work_date DATE NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    last_number INT NOT NULL,
    PRIMARY KEY(work_date,room_number)
);

CREATE TABLE invoice_line (
    line_id INT PRIMARY KEY AUTO_INCREMENT,
    invoice_id INT NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    UNIQUE(source_type,source_id),
    FOREIGN KEY(invoice_id) REFERENCES invoice(invoice_id)
);

CREATE TABLE payment_attempt (
    attempt_id VARCHAR(50) PRIMARY KEY,
    invoice_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_transaction_id VARCHAR(100) UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(invoice_id) REFERENCES invoice(invoice_id)
);

CREATE TABLE notification_outbox (
    id INT PRIMARY KEY AUTO_INCREMENT,
    recipient VARCHAR(100) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    actor_id INT NULL,
    action VARCHAR(60) NOT NULL,
    entity_name VARCHAR(60) NOT NULL,
    entity_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Thu hồi kết quả và tổng hợp báo cáo
CREATE TABLE result_recall_request (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    result_id INT NOT NULL,
    requested_by INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by INT NULL,
    reviewed_at DATETIME NULL,
    review_note VARCHAR(1000) NULL,
    approval_file_url VARCHAR(255) NULL,
    pending_result_id INT GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN result_id ELSE NULL END) STORED,
    UNIQUE KEY uq_pending_result_recall(pending_result_id),
    FOREIGN KEY(result_id) REFERENCES test_result(result_id),
    FOREIGN KEY(requested_by) REFERENCES users(user_id),
    FOREIGN KEY(reviewed_by) REFERENCES users(user_id)
);

CREATE TABLE report_daily_summary (
    summary_date DATE PRIMARY KEY,
    appointment_count INT NOT NULL,
    completed_count INT NOT NULL,
    cancelled_count INT NOT NULL,
    no_show_count INT NOT NULL,
    paid_invoice_count INT NOT NULL,
    total_revenue DECIMAL(18,2) NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Quan hệ thành phần của gói khám; dịch vụ/gói đều dùng entity MedicalService.
CREATE TABLE service_package_item (
    package_service_id INT NOT NULL,
    component_service_id INT NOT NULL,
    PRIMARY KEY(package_service_id, component_service_id),
    FOREIGN KEY(package_service_id) REFERENCES service(service_id),
    FOREIGN KEY(component_service_id) REFERENCES service(service_id),
    CHECK(package_service_id <> component_service_id)
);

-- TÀI KHOẢN DEMO: tất cả dùng mật khẩu Demo@1234 (đã băm PBKDF2).
START TRANSACTION;
INSERT INTO roles(role_id,name,description) VALUES
(1,'ADMIN','Quản trị'),(2,'MANAGER','Quản lý'),(3,'CASHIER','Thu ngân'),
(4,'RECEPTIONIST','Lễ tân'),(5,'LAB_TECHNICIAN','Kỹ thuật viên'),
(6,'DOCTOR','Bác sĩ'),(7,'PATIENT','Bệnh nhân'),(8,'DEPARTMENT_HEAD','Trưởng khoa');
INSERT INTO users(user_id,role_id,email,password,full_name,phone_number) VALUES
(1,1,'admin@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Quản trị','0900000001'),
(2,2,'manager@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Quản lý','0900000002'),
(3,3,'cashier@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Thu ngân','0900000003'),
(4,4,'reception@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Lễ tân','0900000004'),
(5,5,'lab@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Kỹ thuật viên','0900000005'),
(6,6,'doctor@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Bác sĩ An','0900000006'),
(7,6,'doctor2@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Bác sĩ Bình','0900000007'),
(8,7,'patient@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Nguyễn Minh Anh','0900000008'),
(9,7,'patient2@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Trần Hoàng','0900000009'),
(10,7,'patient3@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Lê Ngọc','0900000010'),
(11,8,'head@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Trưởng khoa Nội','0900000011'),
(12,7,'patient4@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Phạm Lan','0900000012'),
(13,7,'patient5@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Vũ Hải','0900000013'),
(14,7,'patient6@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Đỗ Mai','0900000014'),
(15,7,'patient7@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Bùi Sơn','0900000015'),
(16,7,'patient8@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Võ Linh','0900000016'),
(17,7,'patient9@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Hoàng Hà','0900000017');

INSERT INTO departments(department_id,name,description) VALUES
(1,'Nội tổng quát','Khám nội khoa'),(2,'Tai Mũi Họng','Khám chuyên khoa tai mũi họng');
INSERT INTO rooms(room_id,department_id,room_number,room_name,room_type) VALUES
(1,1,'P101','Phòng Nội 1','CONSULTATION'),(2,1,'P102','Phòng Nội 2','CONSULTATION'),
(3,2,'P201','Phòng Tai Mũi Họng','CONSULTATION'),(4,1,'LAB01','Phòng xét nghiệm','LAB');
INSERT INTO doctors(doctor_id,department_id,room_number,academic_title,experience_years,rating_average) VALUES
(6,1,'P101','Bác sĩ',8,0),(7,2,'P201','Thạc sĩ',6,5),(11,1,'P102','Trưởng khoa',12,0);
INSERT INTO patients(patient_id,date_of_birth,address,emergency_contact) VALUES
(8,'2000-01-01','Hà Nội','0901111111'),(9,'1995-02-02','Hà Nội','0901111112'),
(10,'1990-03-03','Hà Nội','0901111113'),(12,'1989-04-04','Hà Nội','0901111114'),
(13,'1993-05-05','Hà Nội','0901111115'),(14,'1998-06-06','Hà Nội','0901111116'),
(15,'1991-07-07','Hà Nội','0901111117'),(16,'1999-08-08','Hà Nội','0901111118'),
(17,'1997-09-09','Hà Nội','0901111119');
INSERT INTO service(service_id,department_id,name,description,category,unit_price) VALUES
(1,1,'Khám chuyên khoa','Phí khám ban đầu','CONSULTATION',150000),
(2,1,'Công thức máu','Xét nghiệm máu cơ bản','LAB',90000),
(3,1,'X-quang ngực','Chẩn đoán hình ảnh','IMAGING',120000),
(4,2,'Nội soi Tai Mũi Họng','Khám cận lâm sàng Tai Mũi Họng','IMAGING',200000),
(5,1,'Gói kiểm tra cơ bản','Gồm công thức máu và X-quang ngực','PACKAGE',190000);
INSERT INTO service_package_item(package_service_id,component_service_id) VALUES(5,2),(5,3);
INSERT INTO medication(medication_id,name,active_ingredient,dosage_form,unit,unit_price,stock_quantity) VALUES
(1,'Thuốc A - minh họa','DEMO A','Viên','Viên',1000,496),
(2,'Thuốc B - minh họa','DEMO B','Viên','Viên',2500,300),
(3,'Thuốc C - minh họa','DEMO C','Chai','Chai',25000,100);

-- Ca khám hôm nay, ngày mai và các ngày tiếp theo; ngày được tính lúc nạp file.
INSERT INTO work_schedule(schedule_id,doctor_id,work_date,shift,start_time,end_time,max_slots,booked_count) VALUES
(1,6,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'MORNING','08:00','12:00',24,1),
(2,6,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'AFTERNOON','13:00','17:00',24,0),
(3,6,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'EVENING','18:00','23:59',60,0),
(4,6,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'MORNING','08:00','12:00',24,0),
(5,6,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'AFTERNOON','13:00','17:00',24,0),
(6,6,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'EVENING','18:00','23:59',60,0),
(7,6,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'MORNING','08:00','12:00',24,0),
(8,6,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'AFTERNOON','13:00','17:00',24,0),
(9,6,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'EVENING','18:00','23:59',60,0),
(10,6,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'MORNING','08:00','12:00',24,0),
(11,6,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'AFTERNOON','13:00','17:00',24,0),
(12,6,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'EVENING','18:00','23:59',60,0),
(13,7,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'MORNING','08:00','12:00',24,2),
(14,7,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'AFTERNOON','13:00','17:00',24,0),
(15,7,DATE_ADD(CURDATE(),INTERVAL 0 DAY),'EVENING','18:00','23:59',60,0),
(16,7,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'MORNING','08:00','12:00',24,1),
(17,7,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'AFTERNOON','13:00','17:00',24,0),
(18,7,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'EVENING','18:00','23:59',60,0),
(19,7,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'MORNING','08:00','12:00',24,0),
(20,7,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'AFTERNOON','13:00','17:00',24,0),
(21,7,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'EVENING','18:00','23:59',60,0),
(22,7,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'MORNING','08:00','12:00',24,0),
(23,7,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'AFTERNOON','13:00','17:00',24,0),
(24,7,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'EVENING','18:00','23:59',60,0),
(25,7,DATE_SUB(CURDATE(),INTERVAL 1 DAY),'MORNING','08:00','12:00',24,1),
(26,7,DATE_SUB(CURDATE(),INTERVAL 2 DAY),'MORNING','08:00','12:00',24,0),
(27,7,DATE_SUB(CURDATE(),INTERVAL 3 DAY),'MORNING','08:00','12:00',24,1);

-- patient/doctor để trống lượt khám: dùng đi trọn luồng mới từ đặt lịch đến đánh giá.
-- patient2: đã đặt lịch hôm nay, có thể tiếp nhận ngay.
-- patient4/doctor2: đang khám, đã trả phí xét nghiệm, chờ kỹ thuật viên nhập kết quả.
-- patient5: lịch sử hoàn tất và đã thanh toán. patient6: hoàn tất, còn hóa đơn chưa trả.
INSERT INTO appointment(appointment_id,appointment_code,patient_id,doctor_id,schedule_id,appointment_date,appointment_time,initial_symptoms,consultation_fee,status) VALUES
(1,'AP-DEMO-01',9,6,1,CURDATE(),'09:00','Đau đầu',150000,'BOOKED'),
(2,'AP-DEMO-02',12,7,13,CURDATE(),'08:00','Đau họng',150000,'CHECKED_IN'),
(3,'AP-DEMO-03',13,7,25,DATE_SUB(CURDATE(),INTERVAL 1 DAY),'09:00','Ho',150000,'COMPLETED'),
(4,'AP-DEMO-04',14,7,13,CURDATE(),'09:00','Mệt',150000,'COMPLETED'),
(5,'AP-DEMO-05',15,7,26,DATE_SUB(CURDATE(),INTERVAL 2 DAY),'09:00','Khám định kỳ',150000,'CANCELLED'),
(6,'AP-DEMO-06',16,7,27,DATE_SUB(CURDATE(),INTERVAL 3 DAY),'09:00','Khám định kỳ',150000,'NO_SHOW'),
(7,'AP-DEMO-07',17,7,16,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'09:00','Khám lại',150000,'BOOKED');
INSERT INTO queue(queue_id,appointment_id,patient_id,doctor_id,queue_number,room_assigned,status,work_date) VALUES
(1,2,12,7,1,'P201','IN_CONSULTATION',CURDATE()),
(2,3,13,7,1,'P201','COMPLETED',DATE_SUB(CURDATE(),INTERVAL 1 DAY)),
(3,4,14,7,2,'P201','COMPLETED',CURDATE());
INSERT INTO queue_counter(work_date,room_number,last_number) VALUES
(CURDATE(),'P201',2),(DATE_SUB(CURDATE(),INTERVAL 1 DAY),'P201',1);
INSERT INTO medical_record(record_id,appointment_id,patient_id,doctor_id,symptoms,icd_code,diagnosis,notes,status,closed_at) VALUES
(1,2,12,7,'Đau họng','J00','Viêm mũi họng cấp','Dữ liệu trình bày quy trình','OPEN',NULL),
(2,3,13,7,'Ho','J00','Viêm mũi họng cấp','Lượt khám lịch sử','CLOSED',DATE_SUB(NOW(),INTERVAL 1 DAY)),
(3,4,14,7,'Mệt','R51','Đau đầu','Chờ thanh toán thuốc','CLOSED',NOW());
INSERT INTO clinical_order(order_id,record_id,service_id,instructions,payment_status,status,unit_price) VALUES
(1,1,2,'Thực hiện xét nghiệm theo chỉ định','PAID','PENDING',90000),
(2,2,2,'Đã hoàn tất xét nghiệm','PAID','COMPLETED',90000);
INSERT INTO test_result(result_id,order_id,technician_id,result_data,conclusion) VALUES
(1,2,5,'Kết quả minh họa cho bài trình bày','Đã đối chiếu và duyệt');
INSERT INTO prescription(prescription_id,record_id,advice,payment_status) VALUES
(1,2,'Đơn minh họa, không dùng để điều trị','PAID'),
(2,3,'Đơn minh họa, không dùng để điều trị','PENDING_PAYMENT');
INSERT INTO prescription_detail(id,prescription_id,medication_id,quantity,dosage,unit_price) VALUES
(1,1,1,2,'Liều dùng minh họa',1000),(2,2,1,2,'Liều dùng minh họa',1000);
-- Tồn kho là tồn hiện tại sau khi đã phát hành hai đơn trên, không trừ thêm lúc nạp.
-- Tồn hiện tại 496 đã nằm trong INSERT medication ở trên.
INSERT INTO invoice(invoice_id,invoice_code,record_id,patient_id,cashier_id,total_amount,payment_method,transaction_id,status,paid_at) VALUES
(1,'INV-DEMO-01',1,12,3,240000,'CASH','CASH-DEMO-01','PAID',NOW()),
(2,'INV-DEMO-02',2,13,3,242000,'CASH','CASH-DEMO-02','PAID',DATE_SUB(NOW(),INTERVAL 1 DAY)),
(3,'INV-DEMO-03',3,14,3,152000,'UNSELECTED',NULL,'UNPAID',NULL);
INSERT INTO invoice_line(line_id,invoice_id,source_type,source_id,description,quantity,unit_price) VALUES
(1,1,'CONSULTATION',2,'Phí khám',1,150000),(2,1,'ORDER',1,'Công thức máu',1,90000),
(3,2,'CONSULTATION',3,'Phí khám',1,150000),(4,2,'ORDER',2,'Công thức máu',1,90000),
(5,2,'MEDICATION',1,'Thuốc A',2,1000),(6,3,'CONSULTATION',4,'Phí khám',1,150000),
(7,3,'MEDICATION',2,'Thuốc A',2,1000);
INSERT INTO feedback(record_id,patient_id,doctor_id,rating,comment) VALUES
(2,13,7,5,'Nhân viên hướng dẫn rõ ràng');
-- Điểm trung bình 5 đã nằm trong INSERT doctors theo feedback mẫu.
COMMIT;

SELECT 'Đã tạo database hospital_demo. Mở http://localhost:3000/hospital/login' AS huong_dan;
