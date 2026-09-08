-- Nivya Database Initialization Script
-- Ensures utf8mb4 encoding for full unicode / emoji support
CREATE DATABASE IF NOT EXISTS `nivya_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON `nivya_db`.* TO 'nivya_user'@'%';
FLUSH PRIVILEGES;
