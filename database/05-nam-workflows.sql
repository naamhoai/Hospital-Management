-- Apply ONCE after 01-schema + 04-extensions + 02-seed (or to the existing SDS-aligned database).
-- Preserve every approved/recalled result; only one approved result per clinical order.
USE hospital;
ALTER TABLE test_result ADD status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE test_result ADD INDEX idx_result_order_history(order_id);
ALTER TABLE test_result DROP INDEX order_id;
ALTER TABLE test_result ADD active_order_id INT GENERATED ALWAYS AS (CASE WHEN status='APPROVED' THEN order_id ELSE NULL END) STORED;
ALTER TABLE test_result ADD UNIQUE KEY uq_active_order_result(active_order_id);

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

INSERT INTO roles(name,description) VALUES('DEPARTMENT_HEAD','Trưởng khoa: duyệt thu hồi kết quả trong chuyên khoa phụ trách')
ON DUPLICATE KEY UPDATE description=VALUES(description);
