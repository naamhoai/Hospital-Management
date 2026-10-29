-- OPTIONAL: synthetic demo account for a fresh demo database only. Never use on real patient data.
USE hospital;
INSERT INTO users(role_id,email,password,full_name,phone_number)
SELECT role_id,'head@hospital.test','pbkdf2$210000$Z3JvdXA2LWRlbW8tc2VlZCE=$2/ePogmsO1Oc6GhEiU1HKIfrnQiSRn7XG6yJ4klQuyQ=','Trưởng khoa Nội Demo','0900000011'
FROM roles WHERE name='DEPARTMENT_HEAD' AND NOT EXISTS(SELECT 1 FROM users WHERE email='head@hospital.test');
INSERT INTO doctors(doctor_id,department_id,room_number,academic_title,experience_years)
SELECT user_id,1,'P102','Trưởng khoa',12 FROM users WHERE email='head@hospital.test'
AND NOT EXISTS(SELECT 1 FROM doctors WHERE doctor_id=users.user_id);
