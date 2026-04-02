-- ============================================================
-- SECURE AUDIT LOGGING AND MONITORING SYSTEM
-- OWASP A10 - Insufficient Logging & Monitoring
-- MySQL Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS secure_audit_db;
USE secure_audit_db;

-- -------------------------------------------------------
-- USER table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS USER (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','ANALYST','USER') NOT NULL DEFAULT 'USER',
    account_status ENUM('ACTIVE','INACTIVE','LOCKED','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- AUTHENTICATION table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS AUTHENTICATION (
    auth_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NULL,
    auth_type   ENUM('LOGIN','LOGOUT','TOKEN','MFA') NOT NULL DEFAULT 'LOGIN',
    auth_result ENUM('SUCCESS','FAILURE','BLOCKED') NOT NULL,
    source_ip   VARCHAR(45)  NOT NULL,
    timestamp   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USER(user_id) ON DELETE SET NULL
);

-- -------------------------------------------------------
-- PASSWORD_EVENT table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS PASSWORD_EVENT (
    password_event_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    event_type        ENUM('CHANGE','RESET_REQUEST','RESET_COMPLETE','UPDATE_FAILURE') NOT NULL,
    event_status      ENUM('SUCCESS','FAILURE') NOT NULL,
    timestamp         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USER(user_id) ON DELETE CASCADE
);

-- -------------------------------------------------------
-- USER_EVENT table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS USER_EVENT (
    user_event_id  INT AUTO_INCREMENT PRIMARY KEY,
    target_user_id INT NOT NULL,
    performed_by   INT NOT NULL,
    event_type     ENUM('CREATED','MODIFIED','ROLE_CHANGED','DELETED','LOCKED','UNLOCKED') NOT NULL,
    timestamp      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (target_user_id) REFERENCES USER(user_id) ON DELETE CASCADE,
    FOREIGN KEY (performed_by)   REFERENCES USER(user_id) ON DELETE CASCADE
);

-- -------------------------------------------------------
-- SESSION table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS SESSION (
    session_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    login_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time    DATETIME NULL,
    session_status ENUM('ACTIVE','EXPIRED','TERMINATED') NOT NULL DEFAULT 'ACTIVE',
    source_ip      VARCHAR(45) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES USER(user_id) ON DELETE CASCADE
);

-- -------------------------------------------------------
-- INPUT_VALIDATION_LOG table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS INPUT_VALIDATION_LOG (
    validation_log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NULL,
    input_field       VARCHAR(100) NOT NULL,
    failure_reason    VARCHAR(255) NOT NULL,
    source_ip         VARCHAR(45)  NOT NULL,
    timestamp         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USER(user_id) ON DELETE SET NULL
);

-- -------------------------------------------------------
-- Seed: default admin user  (password: Admin@123)
-- -------------------------------------------------------
INSERT IGNORE INTO USER (username, email, password_hash, role, account_status)
VALUES ('admin', 'admin@audit.local',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'ADMIN', 'ACTIVE');
