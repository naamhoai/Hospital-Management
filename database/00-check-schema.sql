-- Chỉ đọc thông tin cấu trúc. Chọn đúng connection MySQL của ứng dụng trước khi chạy.
USE hospital;
SELECT DATABASE() AS database_name, @@port AS server_port, VERSION() AS mysql_version;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND ((TABLE_NAME='users' AND COLUMN_NAME IN ('password','password_hash','failed_attempts','locked_until'))
    OR (TABLE_NAME='medical_record' AND COLUMN_NAME IN ('appointment_id','queue_id','diagnosis'))
    OR (TABLE_NAME='test_result' AND COLUMN_NAME IN ('status','active_order_id')))
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE()
ORDER BY TABLE_NAME;
