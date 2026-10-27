-- Explicit technical extensions: concurrency, price snapshots, audit, outbox and idempotent payments.
-- Apply once AFTER 01-schema.sql and BEFORE 02-seed.sql.
USE hospital;
ALTER TABLE users ADD failed_attempts INT NOT NULL DEFAULT 0, ADD locked_until DATETIME NULL;
ALTER TABLE patients ADD booking_blocked_until DATE NULL;
ALTER TABLE appointment ADD created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, ADD consultation_fee DECIMAL(12,2) NOT NULL DEFAULT 0, ADD cancelled_at DATETIME NULL,
 ADD active_slot DATETIME GENERATED ALWAYS AS (CASE WHEN status IN ('BOOKED','CHECKED_IN','COMPLETED') THEN TIMESTAMP(appointment_date,appointment_time) ELSE NULL END) STORED,
 ADD UNIQUE KEY uq_doctor_active_slot(doctor_id,active_slot);
ALTER TABLE queue ADD work_date DATE NOT NULL, ADD created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, ADD UNIQUE KEY uq_queue_number(work_date,room_assigned,queue_number);
ALTER TABLE medical_record ADD created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, ADD closed_at DATETIME NULL;
ALTER TABLE clinical_order ADD unit_price DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE test_result ADD approved BOOLEAN NOT NULL DEFAULT TRUE, ADD created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE medication ADD status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE invoice ADD created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, ADD paid_at DATETIME NULL;

CREATE TABLE queue_counter (work_date DATE NOT NULL, room_number VARCHAR(20) NOT NULL, last_number INT NOT NULL, PRIMARY KEY(work_date,room_number));
CREATE TABLE invoice_line (line_id INT PRIMARY KEY AUTO_INCREMENT, invoice_id INT NOT NULL, source_type VARCHAR(20) NOT NULL, source_id INT NOT NULL, description VARCHAR(255) NOT NULL, quantity INT NOT NULL, unit_price DECIMAL(12,2) NOT NULL, UNIQUE(source_type,source_id), FOREIGN KEY(invoice_id) REFERENCES invoice(invoice_id));
CREATE TABLE payment_attempt (attempt_id VARCHAR(50) PRIMARY KEY, invoice_id INT NOT NULL, amount DECIMAL(12,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', gateway_transaction_id VARCHAR(100) UNIQUE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(invoice_id) REFERENCES invoice(invoice_id));
CREATE TABLE notification_outbox (id INT PRIMARY KEY AUTO_INCREMENT, recipient VARCHAR(100) NOT NULL, subject VARCHAR(255) NOT NULL, body TEXT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempts INT NOT NULL DEFAULT 0, last_error VARCHAR(255), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE audit_log (id INT PRIMARY KEY AUTO_INCREMENT, actor_id INT NULL, action VARCHAR(60) NOT NULL, entity_name VARCHAR(60) NOT NULL, entity_id INT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);
ALTER TABLE feedback ADD created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
