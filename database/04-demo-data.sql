-- Dữ liệu giả lập cho tiếp nhận, khám, xét nghiệm, thanh toán và báo cáo.
-- Chạy sau 01-schema.sql và 02-seed.sql. Không xóa lượt khám đã có.

-- Bộ ca giả lập có thể nạp lại trong ngày.
-- Chỉ bổ sung; không xóa lượt khám cũ hoặc đặt lại trạng thái đã xử lý.
-- Các mã DEMO đã có trong ngày được bỏ qua, kể cả ca bạn đã thao tác.
-- Dùng ngày Việt Nam; tài khoản nạp demo cần quyền CREATE/DROP ROUTINE.
USE hospital;
SET NAMES utf8mb4;
SET SESSION time_zone = '+07:00';

DELIMITER $$
DROP PROCEDURE IF EXISTS seed_demo_main_flow$$
CREATE PROCEDURE seed_demo_main_flow()
BEGIN
    DECLARE n INT DEFAULT 1;
    DECLARE d INT DEFAULT 0;
    DECLARE doc INT;
    DECLARE patient INT;
    DECLARE schedule INT;
    DECLARE appt INT;
    DECLARE record_no INT;
    DECLARE order_no INT;
    DECLARE invoice_no INT;
    DECLARE rx INT;
    DECLARE medication_no INT;
    DECLARE lab_service INT;
    DECLARE cashier INT;
    DECLARE technician INT;
    DECLARE seq INT;
    DECLARE offset_days INT;
    DECLARE day_of_visit DATE;
    DECLARE slot_time TIME;
    DECLARE room_no VARCHAR(20);
    DECLARE code VARCHAR(50);
    DECLARE appt_status VARCHAR(20);
    DECLARE fee DECIMAL(12,2);
    DECLARE lab_fee DECIMAL(12,2);
    DECLARE pass_hash VARCHAR(255) DEFAULT 'pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=';
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    IF (SELECT COUNT(*) FROM departments WHERE department_id IN (1,2) AND status='ACTIVE') <> 2
       OR (SELECT COUNT(*) FROM rooms WHERE room_number IN ('P101','P201') AND status='ACTIVE') <> 2
       OR NOT EXISTS(SELECT 1 FROM roles WHERE name='PATIENT')
       OR NOT EXISTS(SELECT 1 FROM users WHERE email='lab@hospital.test')
       OR NOT EXISTS(SELECT 1 FROM users WHERE email='cashier@hospital.test')
       OR NOT EXISTS(SELECT 1 FROM service WHERE category='CONSULTATION' AND status='ACTIVE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing base demo catalog/accounts: load 02-seed first; do not recreate an existing schema.';
    END IF;

    -- Hai bác sĩ demo độc lập; giữ nguyên các bác sĩ, bệnh nhân và hàng đợi đã có.
    INSERT INTO users(role_id,email,password,full_name,phone_number)
    SELECT role_id,'demo.doctor@hospital.test',pass_hash,'Bác sĩ Demo Nội - luồng chính','0911000001'
    FROM roles WHERE name='DOCTOR'
    AND NOT EXISTS(SELECT 1 FROM users WHERE email='demo.doctor@hospital.test');
    INSERT INTO users(role_id,email,password,full_name,phone_number)
    SELECT role_id,'demo.doctor2@hospital.test',pass_hash,'Bác sĩ Demo TMH - xét nghiệm','0911000002'
    FROM roles WHERE name='DOCTOR'
    AND NOT EXISTS(SELECT 1 FROM users WHERE email='demo.doctor2@hospital.test');
    INSERT INTO doctors(doctor_id,department_id,room_number,academic_title,experience_years)
    SELECT user_id,IF(email='demo.doctor@hospital.test',1,2),
           IF(email='demo.doctor@hospital.test','P101','P201'),'Bác sĩ',5
    FROM users u WHERE email IN ('demo.doctor@hospital.test','demo.doctor2@hospital.test')
    AND NOT EXISTS(SELECT 1 FROM doctors WHERE doctor_id=u.user_id);

    WHILE n <= 12 DO
        INSERT INTO users(role_id,email,password,full_name,phone_number)
        SELECT role_id,CONCAT('demo',LPAD(n,2,'0'),'@hospital.test'),pass_hash,
               CONCAT('Bệnh nhân DEMO ',LPAD(n,2,'0')),CONCAT('09120000',LPAD(n,2,'0'))
        FROM roles WHERE name='PATIENT'
        AND NOT EXISTS(SELECT 1 FROM users WHERE email=CONCAT('demo',LPAD(n,2,'0'),'@hospital.test'));
        INSERT INTO patients(patient_id,date_of_birth,address,emergency_contact)
        SELECT user_id,DATE_SUB('2000-01-01',INTERVAL n YEAR),'Dữ liệu giả lập SWD392','0912999999'
        FROM users u WHERE email=CONCAT('demo',LPAD(n,2,'0'),'@hospital.test')
        AND NOT EXISTS(SELECT 1 FROM patients WHERE patient_id=u.user_id);
        SET n=n+1;
    END WHILE;

    INSERT INTO service(department_id,name,category,unit_price)
    SELECT 1,'DEMO - Xét nghiệm tổng quát','LAB',90000
    WHERE NOT EXISTS(SELECT 1 FROM service WHERE name='DEMO - Xét nghiệm tổng quát');
    INSERT INTO medication(name,active_ingredient,dosage_form,unit,unit_price,stock_quantity)
    SELECT 'DEMO - Thuốc minh họa 1','DEMO ONLY','Viên','Viên',1000,1000
    WHERE NOT EXISTS(SELECT 1 FROM medication WHERE name='DEMO - Thuốc minh họa 1');
    INSERT INTO medication(name,active_ingredient,dosage_form,unit,unit_price,stock_quantity)
    SELECT 'DEMO - Thuốc minh họa 2','DEMO ONLY','Viên','Viên',2000,1000
    WHERE NOT EXISTS(SELECT 1 FROM medication WHERE name='DEMO - Thuốc minh họa 2');
    SELECT service_id,unit_price INTO lab_service,lab_fee FROM service
      WHERE name='DEMO - Xét nghiệm tổng quát' AND status='ACTIVE' LIMIT 1;
    SELECT medication_id INTO medication_no FROM medication WHERE name='DEMO - Thuốc minh họa 1' LIMIT 1;
    SELECT unit_price INTO fee FROM service WHERE category='CONSULTATION' AND status='ACTIVE' ORDER BY service_id LIMIT 1;
    SELECT user_id INTO cashier FROM users WHERE email='cashier@hospital.test';
    SELECT user_id INTO technician FROM users WHERE email='lab@hospital.test';

    -- Availability for today and the following 8 days, three shifts/day, 30 places/shift.
    -- No overlapping shifts are added if someone has manually configured a demo doctor.
    WHILE d <= 8 DO
        INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots)
        SELECT u.user_id,DATE_ADD(CURDATE(),INTERVAL d DAY),v.shift,v.starts,v.ends,30
        FROM users u JOIN (
            SELECT 'MORNING' shift,TIME('08:00') starts,TIME('12:00') ends UNION ALL
            SELECT 'AFTERNOON',TIME('13:00'),TIME('17:00') UNION ALL
            SELECT 'EVENING',TIME('18:00'),TIME('23:59')
        ) v WHERE u.email IN ('demo.doctor@hospital.test','demo.doctor2@hospital.test')
        AND NOT EXISTS(SELECT 1 FROM work_schedule w WHERE w.doctor_id=u.user_id
            AND w.work_date=DATE_ADD(CURDATE(),INTERVAL d DAY)
            AND (w.shift=v.shift OR (w.status<>'CANCELLED' AND w.start_time<v.ends AND w.end_time>v.starts)));
        SET d=d+1;
    END WHILE;

    SET n=2;
    WHILE n <= 12 DO
        SET code=CONCAT('DEMO-',DATE_FORMAT(CURDATE(),'%Y%m%d'),'-C',LPAD(n,2,'0'));
        -- demo01 intentionally has NO appointment: use it to test booking from the beginning.
        IF NOT EXISTS(SELECT 1 FROM appointment WHERE appointment_code=code) THEN
            SET offset_days=CASE n WHEN 5 THEN -1 WHEN 6 THEN -2 WHEN 7 THEN 2
                WHEN 8 THEN -3 WHEN 9 THEN -4 WHEN 10 THEN -7 WHEN 11 THEN -15 WHEN 12 THEN -35 ELSE 0 END;
            SET day_of_visit=DATE_ADD(CURDATE(),INTERVAL offset_days DAY);
            SET slot_time=IF(n=3,TIME('10:00'),TIME('09:04'));
            SELECT user_id INTO patient FROM users WHERE email=CONCAT('demo',LPAD(n,2,'0'),'@hospital.test');
            SELECT user_id INTO doc FROM users WHERE email=IF(n=4,'demo.doctor2@hospital.test','demo.doctor@hospital.test');
            SELECT room_number INTO room_no FROM doctors WHERE doctor_id=doc;
            INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots)
            SELECT doc,day_of_visit,'MORNING','08:00','12:00',30
            WHERE NOT EXISTS(SELECT 1 FROM work_schedule WHERE doctor_id=doc AND work_date=day_of_visit AND shift='MORNING');
            SET schedule=NULL;
            SELECT schedule_id INTO schedule FROM work_schedule
              WHERE doctor_id=doc AND work_date=day_of_visit AND shift='MORNING' AND status='AVAILABLE';
            IF schedule IS NULL THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo schedule is unavailable. Existing schedule preserved; inspect the demo doctor schedule.';
            END IF;
            SET appt_status=CASE WHEN n IN (2,7) THEN 'BOOKED' WHEN n IN (3,4) THEN 'CHECKED_IN'
                WHEN n=8 THEN 'NO_SHOW' WHEN n=9 THEN 'CANCELLED' ELSE 'COMPLETED' END;
            INSERT INTO appointment(appointment_code,patient_id,doctor_id,schedule_id,appointment_date,
                appointment_time,initial_symptoms,status,consultation_fee,cancelled_at,created_at)
            VALUES(code,patient,doc,schedule,day_of_visit,slot_time,'DEMO - triệu chứng minh họa',appt_status,
                fee,IF(n=9,TIMESTAMP(day_of_visit,'07:00'),NULL),TIMESTAMP(DATE_SUB(day_of_visit,INTERVAL 1 DAY),'10:00'));
            SET appt=LAST_INSERT_ID();
            IF n<>9 THEN UPDATE work_schedule SET booked_count=booked_count+1 WHERE schedule_id=schedule; END IF;

            IF n IN (3,4,5,6,10,11,12) THEN
                INSERT INTO queue_counter(work_date,room_number,last_number)
                  VALUES(day_of_visit,room_no,0) ON DUPLICATE KEY UPDATE last_number=last_number;
                UPDATE queue_counter SET last_number=GREATEST(last_number,
                    COALESCE((SELECT MAX(q.queue_number) FROM queue q WHERE q.work_date=day_of_visit AND q.room_assigned=room_no),0))+1
                    WHERE work_date=day_of_visit AND room_number=room_no;
                SELECT last_number INTO seq FROM queue_counter WHERE work_date=day_of_visit AND room_number=room_no;
                INSERT INTO queue(appointment_id,patient_id,doctor_id,queue_number,room_assigned,priority_type,status,work_date,created_at)
                VALUES(appt,patient,doc,seq,room_no,'STANDARD',
                    CASE n WHEN 3 THEN 'WAITING' WHEN 4 THEN 'IN_CONSULTATION' ELSE 'COMPLETED' END,day_of_visit,TIMESTAMP(day_of_visit,'08:50'));
            END IF;
            IF n IN (4,5,6,10,11,12) THEN
                INSERT INTO medical_record(appointment_id,patient_id,doctor_id,symptoms,icd_code,diagnosis,notes,status,created_at,closed_at)
                VALUES(appt,patient,doc,'DEMO - triệu chứng giả lập','J00','DEMO - Chẩn đoán minh họa',
                    'Dữ liệu học tập; không dùng làm hướng dẫn điều trị.',IF(n=4,'OPEN','CLOSED'),
                    TIMESTAMP(day_of_visit,'09:10'),IF(n=4,NULL,TIMESTAMP(day_of_visit,'10:30')));
                SET record_no=LAST_INSERT_ID();
                INSERT INTO clinical_order(record_id,service_id,order_date,instructions,payment_status,status,unit_price)
                VALUES(record_no,lab_service,TIMESTAMP(day_of_visit,'09:15'),'DEMO - kiểm thử quy trình',
                    IF(n=4,'PENDING_PAYMENT','PAID'),IF(n=4,'PENDING','COMPLETED'),lab_fee);
                SET order_no=LAST_INSERT_ID();
                IF n<>4 THEN
                    INSERT INTO test_result(order_id,technician_id,result_data,conclusion,approved,status,created_at)
                    VALUES(order_no,technician,'DEMO - Kết quả xét nghiệm giả lập','DEMO - đã có kết quả',TRUE,'APPROVED',TIMESTAMP(day_of_visit,'10:00'));
                    INSERT INTO prescription(record_id,prescribed_date,advice,payment_status)
                    VALUES(record_no,TIMESTAMP(day_of_visit,'10:15'),'DEMO ONLY - không phải đơn điều trị',IF(n=5,'PENDING_PAYMENT','PAID'));
                    SET rx=LAST_INSERT_ID();
                    INSERT INTO prescription_detail(prescription_id,medication_id,quantity,dosage,unit_price)
                    VALUES(rx,medication_no,3,'DEMO - liều minh họa',1000);
                    UPDATE medication SET stock_quantity=stock_quantity-3 WHERE medication_id=medication_no;
                END IF;

                -- First bill covers consultation + lab. Medication is billed separately, like the code.
                INSERT INTO invoice(invoice_code,record_id,patient_id,cashier_id,total_amount,payment_method,status,created_at,paid_at)
                VALUES(CONCAT(code,'-LAB'),record_no,patient,cashier,fee+lab_fee,
                    IF(n=4,'UNSELECTED','CASH'),IF(n=4,'UNPAID','PAID'),TIMESTAMP(day_of_visit,'09:20'),IF(n=4,NULL,TIMESTAMP(day_of_visit,'09:25')));
                SET invoice_no=LAST_INSERT_ID();
                INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price)
                VALUES(invoice_no,'CONSULTATION',appt,'DEMO - Phí khám',1,fee),
                      (invoice_no,'ORDER',order_no,'DEMO - Xét nghiệm tổng quát',1,lab_fee);
                IF n<>4 THEN
                    INSERT INTO invoice(invoice_code,record_id,patient_id,cashier_id,total_amount,payment_method,status,created_at,paid_at)
                    VALUES(CONCAT(code,'-RX'),record_no,patient,cashier,3000,
                        IF(n=5,'UNSELECTED','CASH'),IF(n=5,'UNPAID','PAID'),TIMESTAMP(day_of_visit,'10:20'),IF(n=5,NULL,TIMESTAMP(day_of_visit,'10:25')));
                    SET invoice_no=LAST_INSERT_ID();
                    INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price)
                    SELECT invoice_no,'MEDICATION',id,'DEMO - Thuốc minh họa 1',quantity,unit_price FROM prescription_detail WHERE prescription_id=rx;
                END IF;
                IF n IN (10,11,12) THEN
                    INSERT INTO feedback(record_id,patient_id,doctor_id,rating,comment,created_at)
                    VALUES(record_no,patient,doc,IF(n=11,4,5),'DEMO - đánh giá giả lập',TIMESTAMP(day_of_visit,'11:00'));
                    UPDATE doctors SET rating_average=(SELECT AVG(f.rating) FROM feedback f WHERE f.doctor_id=doc) WHERE doctor_id=doc;
                END IF;
            END IF;
        END IF;
        SET n=n+1;
    END WHILE;
    COMMIT;
END$$
CALL seed_demo_main_flow()$$
DROP PROCEDURE seed_demo_main_flow$$
DELIMITER ;

-- Liệt kê ID thật để mở đúng bệnh án, hóa đơn và kết quả khi demo.
SELECT u.email,u.full_name,a.appointment_code,a.appointment_date,a.status appointment_status,
    q.queue_id,q.status queue_status,m.record_id,m.status record_status,
    i.invoice_code,i.total_amount,i.status invoice_status
FROM users u LEFT JOIN appointment a ON a.patient_id=u.user_id
LEFT JOIN queue q ON q.appointment_id=a.appointment_id
LEFT JOIN medical_record m ON m.appointment_id=a.appointment_id
LEFT JOIN invoice i ON i.record_id=m.record_id
WHERE u.email REGEXP '^demo[0-9]{2}@hospital[.]test$'
ORDER BY u.email,a.appointment_date DESC,i.invoice_id;

-- Bốn phòng khám demo độc lập, mỗi phòng có bệnh nhân mới và lịch đặt sẵn.
DELIMITER $$
DROP PROCEDURE IF EXISTS seed_demo_clinics$$
CREATE PROCEDURE seed_demo_clinics()
BEGIN
    DECLARE clinic_no INT DEFAULT 1;
    DECLARE patient_no INT;
    DECLARE day_offset INT;
    DECLARE clinic_key VARCHAR(10);
    DECLARE clinic_name VARCHAR(30);
    DECLARE doctor_no INT;
    DECLARE patient_id_no INT;
    DECLARE schedule_no INT;
    DECLARE room_code VARCHAR(20);
    DECLARE visit_day DATE;
    DECLARE appointment_code_no VARCHAR(50);
    DECLARE consultation_fee_no DECIMAL(12,2);
    DECLARE pass_hash VARCHAR(255) DEFAULT 'pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=';
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    START TRANSACTION;
    SELECT unit_price INTO consultation_fee_no FROM service
    WHERE category='CONSULTATION' AND status='ACTIVE' ORDER BY service_id LIMIT 1;
    IF consultation_fee_no IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing active consultation service.';
    END IF;
    WHILE clinic_no<=4 DO
        SET clinic_key=CONCAT('clinic',LPAD(clinic_no,2,'0'));
        SET clinic_name=CONCAT('Phòng khám ',LPAD(clinic_no,2,'0'));
        SET room_code=CONCAT('DEMO-',UPPER(clinic_key));
        INSERT INTO rooms(department_id,room_number,room_name,room_type)
        SELECT 1,room_code,CONCAT('Phòng khám demo - ',clinic_name),'CONSULTATION'
        WHERE NOT EXISTS(SELECT 1 FROM rooms WHERE room_number=room_code);
        INSERT INTO users(role_id,email,password,full_name,phone_number)
        SELECT role_id,CONCAT(clinic_key,'.doctor@hospital.test'),pass_hash,
            CONCAT('Bác sĩ demo - ',clinic_name),CONCAT('092100000',clinic_no)
        FROM roles WHERE name='DOCTOR'
        AND NOT EXISTS(SELECT 1 FROM users WHERE email=CONCAT(clinic_key,'.doctor@hospital.test'));
        SELECT user_id INTO doctor_no FROM users WHERE email=CONCAT(clinic_key,'.doctor@hospital.test');
        INSERT INTO doctors(doctor_id,department_id,room_number,academic_title,experience_years)
        SELECT doctor_no,1,room_code,'Bác sĩ Demo',5
        WHERE NOT EXISTS(SELECT 1 FROM doctors WHERE doctor_id=doctor_no);

        SET day_offset=0;
        WHILE day_offset<=8 DO
            INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots)
            SELECT doctor_no,DATE_ADD(CURDATE(),INTERVAL day_offset DAY),v.shift,v.starts,v.ends,30
            FROM (
                SELECT 'MORNING' shift,TIME('08:00') starts,TIME('12:00') ends UNION ALL
                SELECT 'AFTERNOON',TIME('13:00'),TIME('17:00') UNION ALL
                SELECT 'EVENING',TIME('18:00'),TIME('23:59')
            ) v WHERE NOT EXISTS(SELECT 1 FROM work_schedule w WHERE w.doctor_id=doctor_no
                AND w.work_date=DATE_ADD(CURDATE(),INTERVAL day_offset DAY)
                AND (w.shift=v.shift OR (w.status<>'CANCELLED' AND w.start_time<v.ends AND w.end_time>v.starts)));
            SET day_offset=day_offset+1;
        END WHILE;

        SET patient_no=1;
        WHILE patient_no<=4 DO
            INSERT INTO users(role_id,email,password,full_name,phone_number,gender)
            SELECT role_id,CONCAT(clinic_key,LPAD(patient_no,2,'0'),'@hospital.test'),pass_hash,
                CONCAT('Bệnh nhân demo ',clinic_name,' ',LPAD(patient_no,2,'0')),
                CONCAT('0922',clinic_no,'000',LPAD(patient_no,2,'0')),IF(MOD(patient_no,2)=0,'FEMALE','MALE')
            FROM roles WHERE name='PATIENT'
            AND NOT EXISTS(SELECT 1 FROM users WHERE email=CONCAT(clinic_key,LPAD(patient_no,2,'0'),'@hospital.test'));
            SELECT user_id INTO patient_id_no FROM users WHERE email=CONCAT(clinic_key,LPAD(patient_no,2,'0'),'@hospital.test');
            INSERT INTO patients(patient_id,identity_card,date_of_birth,address,emergency_contact)
            SELECT patient_id_no,CONCAT('DEMO-',UPPER(clinic_key),'-',patient_no),
                DATE_SUB('2000-01-01',INTERVAL patient_no YEAR),'Dữ liệu giả lập - test nhóm SWD392','0922999999'
            WHERE NOT EXISTS(SELECT 1 FROM patients WHERE patient_id=patient_id_no);

            IF patient_no IN (2,3) THEN
                SET visit_day=DATE_ADD(CURDATE(),INTERVAL (IF(patient_no=2,0,2)) DAY);
                SET appointment_code_no=CONCAT('DEMO-CLINIC-',DATE_FORMAT(CURDATE(),'%Y%m%d'),'-',UPPER(clinic_key),'-',patient_no);
                IF NOT EXISTS(SELECT 1 FROM appointment WHERE appointment_code=appointment_code_no) THEN
                    SET schedule_no=NULL;
                    SELECT schedule_id INTO schedule_no FROM work_schedule
                    WHERE doctor_id=doctor_no AND work_date=visit_day AND shift='MORNING' AND status='AVAILABLE';
                    IF schedule_no IS NULL THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Team demo schedule unavailable; existing schedule preserved.';
                    END IF;
                    INSERT INTO appointment(appointment_code,patient_id,doctor_id,schedule_id,appointment_date,
                        appointment_time,initial_symptoms,status,consultation_fee)
                    VALUES(appointment_code_no,patient_id_no,doctor_no,schedule_no,visit_day,'09:04',
                        CONCAT('Ca khám demo - ',clinic_name),'BOOKED',consultation_fee_no);
                    UPDATE work_schedule SET booked_count=booked_count+1 WHERE schedule_id=schedule_no;
                END IF;
            END IF;
            SET patient_no=patient_no+1;
        END WHILE;
        SET clinic_no=clinic_no+1;
    END WHILE;
    COMMIT;
END$$
CALL seed_demo_clinics()$$
DROP PROCEDURE seed_demo_clinics$$
DELIMITER ;

SELECT u.email,u.full_name,doc.room_number,a.appointment_code,a.appointment_date,a.status
FROM users u LEFT JOIN patients p ON p.patient_id=u.user_id
LEFT JOIN doctors doc ON doc.doctor_id=u.user_id
LEFT JOIN appointment a ON a.patient_id=p.patient_id
WHERE u.email REGEXP '^(clinic01|clinic02|clinic03|clinic04)([0-9]{2}|[.]doctor)@hospital[.]test$'
ORDER BY u.email,a.appointment_date DESC;

-- Bổ sung các ca đã tiếp nhận để danh sách trên web có dữ liệu.
-- Bác sĩ hoạt động có hàng đợi, lịch sử bệnh án, đơn thuốc và hóa đơn.
-- Giữ nguyên ca đang khám; không tạo hai ca đang khám cho cùng bác sĩ.
USE hospital;
SET NAMES utf8mb4;
SET SESSION time_zone='+07:00';
DELIMITER $$
DROP PROCEDURE IF EXISTS seed_demo_web_workflows$$
CREATE PROCEDURE seed_demo_web_workflows()
BEGIN
    DECLARE finished INT DEFAULT 0;
    DECLARE doc INT;
    DECLARE doc_email VARCHAR(100);
    DECLARE doc_name VARCHAR(100);
    DECLARE dept INT;
    DECLARE room_no VARCHAR(20);
    DECLARE prefix VARCHAR(30);
    DECLARE n INT;
    DECLARE p INT;
    DECLARE sched INT;
    DECLARE appt INT;
    DECLARE rec INT;
    DECLARE ord INT;
    DECLARE result_no INT;
    DECLARE rx INT;
    DECLARE inv INT;
    DECLARE tech INT;
    DECLARE cashier INT;
    DECLARE lab_service INT;
    DECLARE med INT;
    DECLARE queue_no INT;
    DECLARE slot_index INT;
    DECLARE capacity INT;
    DECLARE interval_minutes INT;
    DECLARE starts TIME;
    DECLARE slot_time TIME;
    DECLARE visit_day DATE;
    DECLARE code VARCHAR(50);
    DECLARE is_open BOOLEAN;
    DECLARE fee DECIMAL(12,2);
    DECLARE lab_fee DECIMAL(12,2);
    DECLARE med_fee DECIMAL(12,2);
    DECLARE med_name VARCHAR(100);
    DECLARE pass_hash VARCHAR(255) DEFAULT 'pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=';
    DECLARE doctor_cursor CURSOR FOR
        SELECT d.doctor_id,u.email,u.full_name,d.department_id,d.room_number
        FROM doctors d JOIN users u ON u.user_id=d.doctor_id JOIN roles r USING(role_id)
        JOIN rooms rm ON rm.room_number=d.room_number
        WHERE r.name='DOCTOR' AND u.status='ACTIVE' AND rm.status='ACTIVE' ORDER BY d.doctor_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished=1;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    START TRANSACTION;
    SET tech=(SELECT user_id FROM users WHERE email='lab@hospital.test');
    SET cashier=(SELECT user_id FROM users WHERE email='cashier@hospital.test');
    SET lab_service=(SELECT service_id FROM service WHERE name='DEMO - Xét nghiệm tổng quát' AND status='ACTIVE' LIMIT 1);
    SET med=(SELECT medication_id FROM medication WHERE name='DEMO - Thuốc minh họa 1' AND status='ACTIVE' LIMIT 1);
    SET fee=(SELECT unit_price FROM service WHERE category='CONSULTATION' AND status='ACTIVE' ORDER BY service_id LIMIT 1);
    IF tech IS NULL OR cashier IS NULL OR lab_service IS NULL OR med IS NULL OR fee IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing demo catalog/accounts. Run 04-demo-data.sql after 01-schema.sql and 02-seed.sql.';
    END IF;
    SELECT unit_price INTO lab_fee FROM service WHERE service_id=lab_service;
    SELECT unit_price,name INTO med_fee,med_name FROM medication WHERE medication_id=med;
    OPEN doctor_cursor;
    doctor_loop: LOOP
        FETCH doctor_cursor INTO doc,doc_email,doc_name,dept,room_no;
        IF finished=1 THEN LEAVE doctor_loop; END IF;
        SET prefix=CASE doc_email
            WHEN 'doctor@hospital.test' THEN 'an'
            WHEN 'doctor2@hospital.test' THEN 'binh'
            WHEN 'demo.doctor@hospital.test' THEN 'demo.main'
            WHEN 'demo.doctor2@hospital.test' THEN 'demo.tmh'
            WHEN 'clinic01.doctor@hospital.test' THEN 'clinic01'
            WHEN 'clinic02.doctor@hospital.test' THEN 'clinic02'
            WHEN 'clinic03.doctor@hospital.test' THEN 'clinic03'
            WHEN 'clinic04.doctor@hospital.test' THEN 'clinic04'
            ELSE CONCAT('web',doc) END;
        SET n=5;
        WHILE n<=9 DO
            SET visit_day=CASE n WHEN 8 THEN DATE_SUB(CURDATE(),INTERVAL 14 DAY)
                WHEN 9 THEN DATE_SUB(CURDATE(),INTERVAL 1 DAY) ELSE CURDATE() END;
            SET code=CONCAT('WEB-',DATE_FORMAT(CURDATE(),'%Y%m%d'),'-D',doc,'-C',n);
            IF NOT EXISTS(SELECT 1 FROM appointment WHERE appointment_code=code) THEN
                INSERT INTO users(role_id,email,password,full_name,phone_number)
                SELECT role_id,CONCAT(prefix,LPAD(n,2,'0'),'@hospital.test'),pass_hash,
                    CONCAT('BN ',prefix,' ',LPAD(n,2,'0'),' - ',CASE n
                        WHEN 5 THEN 'Chờ khám' WHEN 6 THEN 'Khám và xét nghiệm'
                        WHEN 7 THEN 'Hoàn tất hôm nay' WHEN 8 THEN 'Lịch sử đã trả tiền'
                        ELSE 'Chờ thanh toán thuốc' END),CONCAT('093',LPAD(doc,4,'0'),LPAD(n,3,'0'))
                FROM roles WHERE name='PATIENT' AND NOT EXISTS(
                    SELECT 1 FROM users WHERE email=CONCAT(prefix,LPAD(n,2,'0'),'@hospital.test'));
                SET p=(SELECT user_id FROM users WHERE email=CONCAT(prefix,LPAD(n,2,'0'),'@hospital.test'));
                INSERT INTO patients(patient_id,date_of_birth,address,emergency_contact)
                SELECT p,DATE_SUB('2000-01-01',INTERVAL n YEAR),'Dữ liệu giả lập SWD392','0939999999'
                WHERE NOT EXISTS(SELECT 1 FROM patients WHERE patient_id=p);

                INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots)
                SELECT doc,visit_day,'MORNING','08:00','12:00',30
                WHERE NOT EXISTS(SELECT 1 FROM work_schedule WHERE doctor_id=doc AND work_date=visit_day AND shift='MORNING');
                SET sched=(SELECT schedule_id FROM work_schedule WHERE doctor_id=doc AND work_date=visit_day
                    AND status='AVAILABLE' AND booked_count<max_slots ORDER BY start_time LIMIT 1);
                IF sched IS NULL THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No available demo schedule; existing schedule preserved.';
                END IF;
                SELECT start_time,max_slots,TIMESTAMPDIFF(MINUTE,start_time,end_time) DIV max_slots
                    INTO starts,capacity,interval_minutes FROM work_schedule WHERE schedule_id=sched;
                -- Chọn đúng giờ còn trống theo bước thời gian của ca khám.
                SET slot_index=0;
                slot_loop: LOOP
                    IF slot_index>=capacity OR interval_minutes<1 THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No free appointment slot for web fixtures.';
                    END IF;
                    SET slot_time=ADDTIME(starts,SEC_TO_TIME(slot_index*interval_minutes*60));
                    IF NOT EXISTS(SELECT 1 FROM appointment WHERE doctor_id=doc AND appointment_date=visit_day
                        AND appointment_time=slot_time AND status IN ('BOOKED','CHECKED_IN','COMPLETED')) THEN
                        LEAVE slot_loop;
                    END IF;
                    SET slot_index=slot_index+1;
                END LOOP;
                SET is_open=(n=6 AND NOT EXISTS(SELECT 1 FROM queue WHERE doctor_id=doc AND status='IN_CONSULTATION'));
                INSERT INTO appointment(appointment_code,patient_id,doctor_id,schedule_id,appointment_date,
                    appointment_time,initial_symptoms,status,consultation_fee,created_at)
                VALUES(code,p,doc,sched,visit_day,slot_time,'DEMO - triệu chứng giả lập',
                    IF(n=5 OR is_open,'CHECKED_IN','COMPLETED'),fee,
                    LEAST(TIMESTAMP(visit_day,'07:30'),NOW()));
                SET appt=LAST_INSERT_ID();
                UPDATE work_schedule SET booked_count=booked_count+1 WHERE schedule_id=sched;
                INSERT INTO queue_counter(work_date,room_number,last_number) VALUES(visit_day,room_no,0)
                    ON DUPLICATE KEY UPDATE last_number=last_number;
                UPDATE queue_counter SET last_number=GREATEST(last_number,
                    COALESCE((SELECT MAX(q.queue_number) FROM queue q WHERE q.work_date=visit_day AND q.room_assigned=room_no),0))+1
                    WHERE work_date=visit_day AND room_number=room_no;
                SELECT last_number INTO queue_no FROM queue_counter WHERE work_date=visit_day AND room_number=room_no;
                INSERT INTO queue(appointment_id,patient_id,doctor_id,queue_number,room_assigned,priority_type,status,work_date,created_at)
                VALUES(appt,p,doc,queue_no,room_no,'STANDARD',
                    IF(n=5,'WAITING',IF(is_open,'IN_CONSULTATION','COMPLETED')),visit_day,
                    LEAST(TIMESTAMP(visit_day,'08:00'),NOW()));

                IF n<>5 THEN
                    INSERT INTO medical_record(appointment_id,patient_id,doctor_id,symptoms,icd_code,diagnosis,notes,status,created_at,closed_at)
                    VALUES(appt,p,doc,'DEMO - triệu chứng minh họa','J00','DEMO - Chẩn đoán minh họa',
                        'Dữ liệu học tập, không phải hướng dẫn điều trị.',IF(is_open,'OPEN','CLOSED'),
                        LEAST(TIMESTAMP(visit_day,'08:10'),NOW()),IF(is_open,NULL,LEAST(TIMESTAMP(visit_day,'10:00'),NOW())));
                    SET rec=LAST_INSERT_ID();
                    INSERT INTO clinical_order(record_id,service_id,order_date,instructions,payment_status,status,unit_price)
                    VALUES(rec,lab_service,LEAST(TIMESTAMP(visit_day,'08:15'),NOW()),'DEMO - test trên giao diện',
                        'PAID',IF(is_open,'PENDING','COMPLETED'),lab_fee);
                    SET ord=LAST_INSERT_ID();
                    IF NOT is_open THEN
                        INSERT INTO test_result(order_id,technician_id,result_data,conclusion,approved,status,created_at)
                        VALUES(ord,tech,'DEMO - Kết quả xét nghiệm giả lập','DEMO - đã duyệt kết quả',TRUE,'APPROVED',
                            LEAST(TIMESTAMP(visit_day,'09:00'),NOW()));
                        SET result_no=LAST_INSERT_ID();
                        INSERT INTO prescription(record_id,prescribed_date,advice,payment_status)
                        VALUES(rec,LEAST(TIMESTAMP(visit_day,'09:10'),NOW()),'DEMO ONLY - không phải đơn điều trị',
                            IF(n=9,'PENDING_PAYMENT','PAID'));
                        SET rx=LAST_INSERT_ID();
                        INSERT INTO prescription_detail(prescription_id,medication_id,quantity,dosage,unit_price)
                        VALUES(rx,med,3,'DEMO - liều minh họa',med_fee);
                        UPDATE medication SET stock_quantity=stock_quantity-3 WHERE medication_id=med;
                        IF n=8 THEN
                            INSERT INTO feedback(record_id,patient_id,doctor_id,rating,comment,created_at)
                            VALUES(rec,p,doc,5,'DEMO - đánh giá sau khám',LEAST(TIMESTAMP(visit_day,'11:00'),NOW()));
                            UPDATE doctors SET rating_average=(SELECT AVG(f.rating) FROM feedback f WHERE f.doctor_id=doc) WHERE doctor_id=doc;
                            -- One real pending approval fixture; do not forge an approved PDF/signature.
                            INSERT INTO result_recall_request(result_id,requested_by,reason)
                            VALUES(result_no,tech,'DEMO - đề nghị kiểm tra lại kết quả để test trưởng khoa');
                        END IF;
                    END IF;
                    INSERT INTO invoice(invoice_code,record_id,patient_id,cashier_id,total_amount,payment_method,status,created_at,paid_at)
                    VALUES(CONCAT(code,'-LAB'),rec,p,cashier,fee+lab_fee,'CASH','PAID',
                        LEAST(TIMESTAMP(visit_day,'08:20'),NOW()),LEAST(TIMESTAMP(visit_day,'08:25'),NOW()));
                    SET inv=LAST_INSERT_ID();
                    INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price)
                    VALUES(inv,'CONSULTATION',appt,'DEMO - Phí khám',1,fee),
                        (inv,'ORDER',ord,'DEMO - Xét nghiệm tổng quát',1,lab_fee);
                    IF NOT is_open THEN
                        INSERT INTO invoice(invoice_code,record_id,patient_id,cashier_id,total_amount,payment_method,status,created_at,paid_at)
                        VALUES(CONCAT(code,'-RX'),rec,p,cashier,3*med_fee,IF(n=9,'UNSELECTED','CASH'),IF(n=9,'UNPAID','PAID'),
                            LEAST(TIMESTAMP(visit_day,'09:15'),NOW()),IF(n=9,NULL,LEAST(TIMESTAMP(visit_day,'09:20'),NOW())));
                        SET inv=LAST_INSERT_ID();
                        INSERT INTO invoice_line(invoice_id,source_type,source_id,description,quantity,unit_price)
                        SELECT inv,'MEDICATION',id,med_name,quantity,unit_price FROM prescription_detail WHERE prescription_id=rx;
                    END IF;
                END IF;
            END IF;
            SET n=n+1;
        END WHILE;
    END LOOP;
    CLOSE doctor_cursor;
    COMMIT;
END$$
CALL seed_demo_web_workflows()$$
DROP PROCEDURE seed_demo_web_workflows$$
DELIMITER ;

SELECT d.email doctor,p.email patient,a.appointment_code,q.queue_id,q.status queue_status,
    m.record_id,m.status record_status,o.order_id,o.payment_status,o.status order_status
FROM appointment a JOIN users d ON d.user_id=a.doctor_id JOIN users p ON p.user_id=a.patient_id
JOIN queue q ON q.appointment_id=a.appointment_id
LEFT JOIN medical_record m ON m.appointment_id=a.appointment_id
LEFT JOIN clinical_order o ON o.record_id=m.record_id
WHERE a.appointment_code LIKE CONCAT('WEB-',DATE_FORMAT(CURDATE(),'%Y%m%d'),'-%')
ORDER BY d.email,p.email;
