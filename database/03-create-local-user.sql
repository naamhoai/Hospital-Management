-- Run as a database administrator. Change this local-development password.
CREATE USER IF NOT EXISTS 'hospital'@'localhost' IDENTIFIED BY 'change-me-local';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.* TO 'hospital'@'localhost';
