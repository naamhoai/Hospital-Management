-- Existing MVP database ONLY. Back up first; do NOT run with fresh-install scripts.
-- Technical tables/columns already exist in the MVP and are retained.
USE hospital;
ALTER TABLE users RENAME COLUMN password_hash TO password;
UPDATE medical_record SET diagnosis='' WHERE diagnosis IS NULL;
ALTER TABLE medical_record MODIFY diagnosis VARCHAR(255) NOT NULL;
SET @queue_fk=(SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='medical_record' AND COLUMN_NAME='queue_id' AND REFERENCED_TABLE_NAME='queue' LIMIT 1);
SET @drop_fk=CONCAT('ALTER TABLE medical_record DROP FOREIGN KEY `',@queue_fk,'`');
PREPARE stmt FROM @drop_fk; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE medical_record DROP COLUMN queue_id;
-- Queue context now resolves through queue.appointment_id = medical_record.appointment_id.
-- icd_catalog may be retained for archival purposes; runtime uses the configured CSV catalog.
