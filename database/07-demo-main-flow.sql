-- Synthetic demo data only. Run AFTER 01 -> 04 -> 02 -> 05 -> optional 06.
-- Additive: never deletes/resets existing encounters or changes their states.
-- Same-day reruns skip existing DEMO-YYYYMMDD-* cases, including cases you have edited.
-- CURDATE() uses Vietnam time. Run with a MySQL account allowed to CREATE/DROP ROUTINE.
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

    -- Two independent demo doctors keep existing doctors/patients and their queues untouched.
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

-- Checklist with actual IDs, useful for opening EMR, billing, lab and presentation slides.
SELECT u.email,u.full_name,a.appointment_code,a.appointment_date,a.status appointment_status,
    q.queue_id,q.status queue_status,m.record_id,m.status record_status,
    i.invoice_code,i.total_amount,i.status invoice_status
FROM users u LEFT JOIN appointment a ON a.patient_id=u.user_id
LEFT JOIN queue q ON q.appointment_id=a.appointment_id
LEFT JOIN medical_record m ON m.appointment_id=a.appointment_id
LEFT JOIN invoice i ON i.record_id=m.record_id
WHERE u.email REGEXP '^demo[0-9]{2}@hospital[.]test$'
ORDER BY u.email,a.appointment_date DESC,i.invoice_id;

-- Independent queues for four team members: two fresh patients, one booked today,
-- one booked in two days. Original demo cases above remain available.
DELIMITER $$
DROP PROCEDURE IF EXISTS seed_demo_team_testing$$
CREATE PROCEDURE seed_demo_team_testing()
BEGIN
    DECLARE member_no INT DEFAULT 1;
    DECLARE patient_no INT;
    DECLARE day_offset INT;
    DECLARE member_key VARCHAR(10);
    DECLARE member_name VARCHAR(30);
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
    WHILE member_no<=4 DO
        SET member_key=ELT(member_no,'khanh','trang','kien','nam');
        SET member_name=ELT(member_no,'Khánh','Tráng','Kiên','Nam');
        SET room_code=CONCAT('DEMO-',UPPER(member_key));
        INSERT INTO rooms(department_id,room_number,room_name,room_type)
        SELECT 1,room_code,CONCAT('Phòng test riêng - ',member_name),'CONSULTATION'
        WHERE NOT EXISTS(SELECT 1 FROM rooms WHERE room_number=room_code);
        INSERT INTO users(role_id,email,password,full_name,phone_number)
        SELECT role_id,CONCAT(member_key,'.doctor@hospital.test'),pass_hash,
            CONCAT('Bác sĩ test - ',member_name),CONCAT('092100000',member_no)
        FROM roles WHERE name='DOCTOR'
        AND NOT EXISTS(SELECT 1 FROM users WHERE email=CONCAT(member_key,'.doctor@hospital.test'));
        SELECT user_id INTO doctor_no FROM users WHERE email=CONCAT(member_key,'.doctor@hospital.test');
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
            SELECT role_id,CONCAT(member_key,LPAD(patient_no,2,'0'),'@hospital.test'),pass_hash,
                CONCAT('BN test ',member_name,' ',LPAD(patient_no,2,'0')),
                CONCAT('0922',member_no,'000',LPAD(patient_no,2,'0')),IF(MOD(patient_no,2)=0,'FEMALE','MALE')
            FROM roles WHERE name='PATIENT'
            AND NOT EXISTS(SELECT 1 FROM users WHERE email=CONCAT(member_key,LPAD(patient_no,2,'0'),'@hospital.test'));
            SELECT user_id INTO patient_id_no FROM users WHERE email=CONCAT(member_key,LPAD(patient_no,2,'0'),'@hospital.test');
            INSERT INTO patients(patient_id,identity_card,date_of_birth,address,emergency_contact)
            SELECT patient_id_no,CONCAT('DEMO-',UPPER(member_key),'-',patient_no),
                DATE_SUB('2000-01-01',INTERVAL patient_no YEAR),'Dữ liệu giả lập - test nhóm SWD392','0922999999'
            WHERE NOT EXISTS(SELECT 1 FROM patients WHERE patient_id=patient_id_no);

            IF patient_no IN (2,3) THEN
                SET visit_day=DATE_ADD(CURDATE(),INTERVAL (IF(patient_no=2,0,2)) DAY);
                SET appointment_code_no=CONCAT('DEMO-TEAM-',DATE_FORMAT(CURDATE(),'%Y%m%d'),'-',UPPER(member_key),'-',patient_no);
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
                        CONCAT('DEMO - ca test của ',member_name),'BOOKED',consultation_fee_no);
                    UPDATE work_schedule SET booked_count=booked_count+1 WHERE schedule_id=schedule_no;
                END IF;
            END IF;
            SET patient_no=patient_no+1;
        END WHILE;
        SET member_no=member_no+1;
    END WHILE;
    COMMIT;
END$$
CALL seed_demo_team_testing()$$
DROP PROCEDURE seed_demo_team_testing$$
DELIMITER ;

SELECT u.email,u.full_name,doc.room_number,a.appointment_code,a.appointment_date,a.status
FROM users u LEFT JOIN patients p ON p.patient_id=u.user_id
LEFT JOIN doctors doc ON doc.doctor_id=u.user_id
LEFT JOIN appointment a ON a.patient_id=p.patient_id
WHERE u.email REGEXP '^(khanh|trang|kien|nam)([0-9]{2}|[.]doctor)@hospital[.]test$'
ORDER BY u.email,a.appointment_date DESC;
