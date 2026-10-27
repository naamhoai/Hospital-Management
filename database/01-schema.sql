-- Run once in a new database. Never drops existing data.
CREATE DATABASE IF NOT EXISTS hospital CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital;
CREATE TABLE roles (role_id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(30) NOT NULL UNIQUE, description VARCHAR(255));
CREATE TABLE users (
 user_id BIGINT PRIMARY KEY AUTO_INCREMENT, role_id BIGINT NOT NULL, email VARCHAR(100) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL, full_name VARCHAR(100) NOT NULL, phone_number VARCHAR(20),
 gender VARCHAR(10), status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', failed_attempts INT NOT NULL DEFAULT 0,
 locked_until DATETIME NULL, FOREIGN KEY(role_id) REFERENCES roles(role_id)
);
CREATE TABLE departments (department_id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL UNIQUE, description VARCHAR(255), status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE');
CREATE TABLE rooms (room_id BIGINT PRIMARY KEY AUTO_INCREMENT, department_id BIGINT NOT NULL, room_number VARCHAR(20) NOT NULL UNIQUE, room_name VARCHAR(100) NOT NULL, room_type VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', FOREIGN KEY(department_id) REFERENCES departments(department_id));
CREATE TABLE doctors (doctor_id BIGINT PRIMARY KEY, department_id BIGINT NOT NULL, room_number VARCHAR(20) NOT NULL, academic_title VARCHAR(50), experience_years INT DEFAULT 0, rating_average DECIMAL(3,2) DEFAULT 0, FOREIGN KEY(doctor_id) REFERENCES users(user_id), FOREIGN KEY(department_id) REFERENCES departments(department_id), FOREIGN KEY(room_number) REFERENCES rooms(room_number));
CREATE TABLE patients (patient_id BIGINT PRIMARY KEY, identity_card VARCHAR(20) UNIQUE, insurance_number VARCHAR(30), date_of_birth DATE, address VARCHAR(255), emergency_contact VARCHAR(20), booking_blocked_until DATE, FOREIGN KEY(patient_id) REFERENCES users(user_id));
CREATE TABLE work_schedule (
 schedule_id BIGINT PRIMARY KEY AUTO_INCREMENT, doctor_id BIGINT NOT NULL, work_date DATE NOT NULL,
 shift VARCHAR(20) NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL, max_slots INT NOT NULL,
 booked_count INT NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
 UNIQUE(doctor_id,work_date,shift), CHECK(max_slots>0 AND booked_count>=0 AND booked_count<=max_slots),
 CHECK(end_time>start_time), FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id)
);
CREATE TABLE appointment (
 appointment_id BIGINT PRIMARY KEY AUTO_INCREMENT, appointment_code VARCHAR(50) NOT NULL UNIQUE,
 patient_id BIGINT NOT NULL, doctor_id BIGINT NOT NULL, schedule_id BIGINT NOT NULL,
 appointment_date DATE NOT NULL, appointment_time TIME NOT NULL, initial_symptoms VARCHAR(255),
 reschedule_count INT NOT NULL DEFAULT 0, consultation_fee DECIMAL(12,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, cancelled_at DATETIME NULL,
 active_slot DATETIME GENERATED ALWAYS AS (CASE WHEN status IN ('BOOKED','CHECKED_IN','COMPLETED') THEN TIMESTAMP(appointment_date,appointment_time) ELSE NULL END) STORED,
 UNIQUE(doctor_id,active_slot),
 FOREIGN KEY(patient_id) REFERENCES patients(patient_id), FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id), FOREIGN KEY(schedule_id) REFERENCES work_schedule(schedule_id), INDEX(patient_id,status)
);
CREATE TABLE queue_counter (work_date DATE NOT NULL, room_number VARCHAR(20) NOT NULL, last_number INT NOT NULL, PRIMARY KEY(work_date,room_number));
CREATE TABLE queue (
 queue_id BIGINT PRIMARY KEY AUTO_INCREMENT, appointment_id BIGINT NOT NULL UNIQUE, patient_id BIGINT NOT NULL, doctor_id BIGINT NOT NULL,
 work_date DATE NOT NULL, queue_number INT NOT NULL, room_assigned VARCHAR(20) NOT NULL, priority_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
 status VARCHAR(20) NOT NULL DEFAULT 'WAITING', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(work_date,room_assigned,queue_number), FOREIGN KEY(appointment_id) REFERENCES appointment(appointment_id), FOREIGN KEY(patient_id) REFERENCES patients(patient_id), FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id), FOREIGN KEY(room_assigned) REFERENCES rooms(room_number)
);
CREATE TABLE medical_record (
 record_id BIGINT PRIMARY KEY AUTO_INCREMENT, queue_id BIGINT NOT NULL UNIQUE, appointment_id BIGINT NOT NULL UNIQUE,
 patient_id BIGINT NOT NULL, doctor_id BIGINT NOT NULL, symptoms TEXT, icd_code VARCHAR(20), diagnosis VARCHAR(255), notes TEXT,
 status VARCHAR(20) NOT NULL DEFAULT 'OPEN', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, closed_at DATETIME NULL,
 FOREIGN KEY(queue_id) REFERENCES queue(queue_id), FOREIGN KEY(appointment_id) REFERENCES appointment(appointment_id), FOREIGN KEY(patient_id) REFERENCES patients(patient_id), FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id)
);
CREATE TABLE icd_catalog (code VARCHAR(20) PRIMARY KEY, description VARCHAR(255) NOT NULL);
CREATE TABLE service (service_id BIGINT PRIMARY KEY AUTO_INCREMENT, department_id BIGINT NOT NULL, name VARCHAR(100) NOT NULL, category VARCHAR(30) NOT NULL, unit_price DECIMAL(12,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', CHECK(unit_price>=0), FOREIGN KEY(department_id) REFERENCES departments(department_id));
CREATE TABLE clinical_order (
 order_id BIGINT PRIMARY KEY AUTO_INCREMENT, record_id BIGINT NOT NULL, service_id BIGINT NOT NULL,
 order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, instructions VARCHAR(255), unit_price DECIMAL(12,2) NOT NULL,
 payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT', status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 UNIQUE(record_id,service_id), FOREIGN KEY(record_id) REFERENCES medical_record(record_id), FOREIGN KEY(service_id) REFERENCES service(service_id)
);
CREATE TABLE test_result (result_id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL UNIQUE, technician_id BIGINT NOT NULL, result_data TEXT NOT NULL, conclusion VARCHAR(255), file_url VARCHAR(255), approved BOOLEAN NOT NULL DEFAULT TRUE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(order_id) REFERENCES clinical_order(order_id), FOREIGN KEY(technician_id) REFERENCES users(user_id));
CREATE TABLE medication (medication_id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, active_ingredient VARCHAR(100), dosage_form VARCHAR(50), unit VARCHAR(20) NOT NULL, unit_price DECIMAL(12,2) NOT NULL, stock_quantity INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', CHECK(stock_quantity>=0 AND unit_price>=0));
CREATE TABLE prescription (prescription_id BIGINT PRIMARY KEY AUTO_INCREMENT, record_id BIGINT NOT NULL, prescribed_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, advice VARCHAR(255), payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT', FOREIGN KEY(record_id) REFERENCES medical_record(record_id));
CREATE TABLE prescription_detail (id BIGINT PRIMARY KEY AUTO_INCREMENT, prescription_id BIGINT NOT NULL, medication_id BIGINT NOT NULL, quantity INT NOT NULL, dosage VARCHAR(100) NOT NULL, unit_price DECIMAL(12,2) NOT NULL, CHECK(quantity>0), FOREIGN KEY(prescription_id) REFERENCES prescription(prescription_id), FOREIGN KEY(medication_id) REFERENCES medication(medication_id));
CREATE TABLE invoice (invoice_id BIGINT PRIMARY KEY AUTO_INCREMENT, invoice_code VARCHAR(50) NOT NULL UNIQUE, record_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, cashier_id BIGINT, total_amount DECIMAL(12,2) NOT NULL, payment_method VARCHAR(30), transaction_id VARCHAR(100) UNIQUE, status VARCHAR(20) NOT NULL DEFAULT 'UNPAID', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, paid_at DATETIME NULL, FOREIGN KEY(record_id) REFERENCES medical_record(record_id), FOREIGN KEY(patient_id) REFERENCES patients(patient_id), FOREIGN KEY(cashier_id) REFERENCES users(user_id));
CREATE TABLE invoice_line (line_id BIGINT PRIMARY KEY AUTO_INCREMENT, invoice_id BIGINT NOT NULL, source_type VARCHAR(20) NOT NULL, source_id BIGINT NOT NULL, description VARCHAR(255) NOT NULL, quantity INT NOT NULL, unit_price DECIMAL(12,2) NOT NULL, UNIQUE(source_type,source_id), FOREIGN KEY(invoice_id) REFERENCES invoice(invoice_id));
CREATE TABLE payment_attempt (attempt_id VARCHAR(50) PRIMARY KEY, invoice_id BIGINT NOT NULL, amount DECIMAL(12,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', gateway_transaction_id VARCHAR(100) UNIQUE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(invoice_id) REFERENCES invoice(invoice_id));
CREATE TABLE feedback (feedback_id BIGINT PRIMARY KEY AUTO_INCREMENT, record_id BIGINT NOT NULL UNIQUE, patient_id BIGINT NOT NULL, doctor_id BIGINT NOT NULL, rating INT NOT NULL, comment TEXT, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, CHECK(rating BETWEEN 1 AND 5), FOREIGN KEY(record_id) REFERENCES medical_record(record_id), FOREIGN KEY(patient_id) REFERENCES patients(patient_id), FOREIGN KEY(doctor_id) REFERENCES doctors(doctor_id));
CREATE TABLE notification_outbox (id BIGINT PRIMARY KEY AUTO_INCREMENT, recipient VARCHAR(100) NOT NULL, subject VARCHAR(255) NOT NULL, body TEXT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempts INT NOT NULL DEFAULT 0, last_error VARCHAR(255), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE audit_log (id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_id BIGINT NULL, action VARCHAR(60) NOT NULL, entity_name VARCHAR(60) NOT NULL, entity_id BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);
