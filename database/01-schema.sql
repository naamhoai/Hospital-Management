-- Schema đầy đủ cho DATABASE MỚI: 18 bảng nghiệp vụ + 7 bảng hỗ trợ.
-- Không chạy lại file này trên database đã có dữ liệu.
CREATE DATABASE IF NOT EXISTS hospital CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital;
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
