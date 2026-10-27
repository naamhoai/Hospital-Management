-- Apply ONLY to an older schema lacking appointment.active_slot.
-- A fresh import of 01-schema.sql already includes this constraint.
ALTER TABLE appointment ADD COLUMN active_slot DATETIME GENERATED ALWAYS AS
 (CASE WHEN status IN ('BOOKED','CHECKED_IN','COMPLETED') THEN TIMESTAMP(appointment_date,appointment_time) ELSE NULL END) STORED,
 ADD UNIQUE KEY uq_doctor_active_slot(doctor_id,active_slot);
