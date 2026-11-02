-- Run AFTER 07-demo-main-flow.sql. Synthetic, additive fixtures for actual web lists.
-- Every active DOCTOR gets a waiting queue, medical history, prescriptions and invoices.
-- Existing consultations are preserved; never give a doctor two active consultations.
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
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing demo catalog/accounts. Run 07-demo-main-flow.sql first.';
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
            WHEN 'khanh.doctor@hospital.test' THEN 'khanh'
            WHEN 'trang.doctor@hospital.test' THEN 'trang'
            WHEN 'kien.doctor@hospital.test' THEN 'kien'
            WHEN 'nam.doctor@hospital.test' THEN 'nam'
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
                -- Pick an actual free slot on the shift grid, not an arbitrary appointment time.
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
