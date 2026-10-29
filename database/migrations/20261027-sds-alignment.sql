-- Existing MVP database ONLY. Back up first; do NOT run with fresh-install scripts.
-- Technical tables/columns already exist in the MVP and are retained.
-- Kiểm tra cột trước khi đổi để chạy tiếp được nếu lần cập nhật trước bị dừng giữa chừng.
USE hospital;
SET @old_password=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='users' AND COLUMN_NAME='password_hash');
SET @new_password=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='users' AND COLUMN_NAME='password');
SET @rename_password=IF(@old_password=1 AND @new_password=0,
 'ALTER TABLE users RENAME COLUMN password_hash TO password',
 'SELECT ''SKIP password rename: inspect users columns if login still fails'' AS migration_status');
PREPARE stmt FROM @rename_password; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- RENAME giữ nguyên hash và tài khoản; không đặt lại mật khẩu.
UPDATE medical_record SET diagnosis='' WHERE diagnosis IS NULL;
ALTER TABLE medical_record MODIFY diagnosis VARCHAR(255) NOT NULL;
SET @queue_fk=(SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='medical_record' AND COLUMN_NAME='queue_id' AND REFERENCED_TABLE_NAME='queue' LIMIT 1);
SET @drop_fk=IF(@queue_fk IS NOT NULL,CONCAT('ALTER TABLE medical_record DROP FOREIGN KEY `',@queue_fk,'`'),
 'SELECT ''SKIP old queue foreign key: already absent'' AS migration_status');
PREPARE stmt FROM @drop_fk; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @queue_column=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='medical_record' AND COLUMN_NAME='queue_id');
SET @drop_queue=IF(@queue_column=1,'ALTER TABLE medical_record DROP COLUMN queue_id',
 'SELECT ''SKIP old queue column: already absent'' AS migration_status');
PREPARE stmt FROM @drop_queue; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- Queue context now resolves through queue.appointment_id = medical_record.appointment_id.
-- icd_catalog may be retained for archival purposes; runtime uses the configured CSV catalog.
