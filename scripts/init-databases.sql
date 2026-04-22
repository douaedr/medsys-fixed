CREATE DATABASE IF NOT EXISTS ms_auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ms_patient_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ms_medical_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ms_rdv_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ms_billing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON ms_auth_db.* TO 'medsys'@'%';
GRANT ALL PRIVILEGES ON ms_patient_db.* TO 'medsys'@'%';
GRANT ALL PRIVILEGES ON ms_medical_db.* TO 'medsys'@'%';
GRANT ALL PRIVILEGES ON ms_rdv_db.* TO 'medsys'@'%';
GRANT ALL PRIVILEGES ON ms_billing_db.* TO 'medsys'@'%';
FLUSH PRIVILEGES;
