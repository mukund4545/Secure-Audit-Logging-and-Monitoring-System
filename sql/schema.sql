-- ============================================================
-- SECURE AUDIT LOGGING AND MONITORING SYSTEM
-- OWASP A10 - Insufficient Logging & Monitoring
-- MySQL Schema — Tables + Triggers + Stored Procedures
-- ============================================================

CREATE DATABASE IF NOT EXISTS secure_audit_newdb;
USE secure_audit_newdb;

-- ============================================================
-- SECTION 1: TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS USER (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(100) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           ENUM('ADMIN','ANALYST','USER') NOT NULL DEFAULT 'USER',
    account_status ENUM('ACTIVE','INACTIVE','LOCKED','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS AUTHENTICATION (
    auth_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NULL,
    auth_type   ENUM('LOGIN','LOGOUT','TOKEN','MFA') NOT NULL DEFAULT 'LOGIN',
    auth_result ENUM('SUCCESS','FAILURE','BLOCKED') NOT NULL,
    source_ip   VARCHAR(45)  NOT NULL,
    timestamp   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USER(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS PASSWORD_EVENT (
    password_event_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    event_type        ENUM('CHANGE','RESET_REQUEST','RESET_COMPLETE','UPDATE_FAILURE') NOT NULL,
    event_status      ENUM('SUCCESS','FAILURE') NOT NULL,
    timestamp         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USER(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS USER_EVENT (
    user_event_id  INT AUTO_INCREMENT PRIMARY KEY,
    target_user_id INT NOT NULL,
    performed_by   INT NOT NULL,
    event_type     ENUM('CREATED','MODIFIED','ROLE_CHANGED','DELETED','LOCKED','UNLOCKED') NOT NULL,
    timestamp      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (target_user_id) REFERENCES USER(user_id) ON DELETE CASCADE,
    FOREIGN KEY (performed_by)   REFERENCES USER(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS SESSION (
    session_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    login_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time    DATETIME NULL,
    session_status ENUM('ACTIVE','EXPIRED','TERMINATED') NOT NULL DEFAULT 'ACTIVE',
    source_ip      VARCHAR(45) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES USER(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS INPUT_VALIDATION_LOG (
    validation_log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NULL,
    input_field       VARCHAR(100) NOT NULL,
    failure_reason    VARCHAR(255) NOT NULL,
    source_ip         VARCHAR(45)  NOT NULL,
    timestamp         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USER(user_id) ON DELETE SET NULL
);

-- ============================================================
-- SECTION 2: TRIGGERS
-- ============================================================

DROP TRIGGER IF EXISTS trg_auto_lock_after_failed_logins;
DROP TRIGGER IF EXISTS trg_log_new_user_created;
DROP TRIGGER IF EXISTS trg_log_user_status_change;
DROP TRIGGER IF EXISTS trg_prevent_last_admin_delete;

DELIMITER $$

-- TRIGGER 1: Auto-lock after 5 consecutive failed logins
CREATE TRIGGER trg_auto_lock_after_failed_logins
AFTER INSERT ON AUTHENTICATION
FOR EACH ROW
BEGIN
    DECLARE fail_count INT DEFAULT 0;
    IF NEW.auth_type = 'LOGIN' AND NEW.auth_result = 'FAILURE' AND NEW.user_id IS NOT NULL THEN
        SELECT COUNT(*) INTO fail_count
        FROM AUTHENTICATION
        WHERE user_id     = NEW.user_id
          AND auth_type   = 'LOGIN'
          AND auth_result = 'FAILURE'
          AND timestamp   >= (NOW() - INTERVAL 10 MINUTE);

        IF fail_count >= 5 THEN
            UPDATE USER SET account_status = 'LOCKED'
            WHERE user_id = NEW.user_id AND account_status = 'ACTIVE';
            IF ROW_COUNT() > 0 THEN
                INSERT INTO USER_EVENT (target_user_id, performed_by, event_type)
                VALUES (NEW.user_id, NEW.user_id, 'LOCKED');
            END IF;
        END IF;
    END IF;
END$$

-- TRIGGER 2: Auto-log USER_EVENT on new user insert
CREATE TRIGGER trg_log_new_user_created
AFTER INSERT ON USER
FOR EACH ROW
BEGIN
    DECLARE already_logged INT DEFAULT 0;
    SELECT COUNT(*) INTO already_logged FROM USER_EVENT
    WHERE target_user_id = NEW.user_id AND event_type = 'CREATED';
    IF already_logged = 0 THEN
        INSERT INTO USER_EVENT (target_user_id, performed_by, event_type)
        VALUES (NEW.user_id, NEW.user_id, 'CREATED');
    END IF;
END$$

-- TRIGGER 3: Auto-log status/role changes
CREATE TRIGGER trg_log_user_status_change
AFTER UPDATE ON USER
FOR EACH ROW
BEGIN
    DECLARE ev_type VARCHAR(20);
    IF OLD.account_status <> NEW.account_status THEN
        SET ev_type = CASE NEW.account_status
            WHEN 'LOCKED' THEN 'LOCKED'
            WHEN 'ACTIVE' THEN 'UNLOCKED'
            ELSE 'MODIFIED'
        END;
        INSERT INTO USER_EVENT (target_user_id, performed_by, event_type)
        VALUES (NEW.user_id, NEW.user_id, ev_type);
    END IF;
    IF OLD.role <> NEW.role THEN
        INSERT INTO USER_EVENT (target_user_id, performed_by, event_type)
        VALUES (NEW.user_id, NEW.user_id, 'ROLE_CHANGED');
    END IF;
END$$

-- TRIGGER 4: Prevent deletion of last ADMIN
CREATE TRIGGER trg_prevent_last_admin_delete
BEFORE DELETE ON USER
FOR EACH ROW
BEGIN
    DECLARE admin_count INT DEFAULT 0;
    IF OLD.role = 'ADMIN' THEN
        SELECT COUNT(*) INTO admin_count FROM USER
        WHERE role = 'ADMIN' AND account_status = 'ACTIVE' AND user_id <> OLD.user_id;
        IF admin_count = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot delete the last active ADMIN account.';
        END IF;
    END IF;
END$$

DELIMITER ;

-- ============================================================
-- SECTION 3: STORED PROCEDURES
-- ============================================================

DROP PROCEDURE IF EXISTS sp_register_user;
DROP PROCEDURE IF EXISTS sp_change_password;
DROP PROCEDURE IF EXISTS sp_get_user_audit_report;
DROP PROCEDURE IF EXISTS sp_terminate_expired_sessions;
DROP PROCEDURE IF EXISTS sp_login_attempt;

DELIMITER $$

-- PROCEDURE 1: Register user atomically
CREATE PROCEDURE sp_register_user(
    IN  p_username      VARCHAR(50),
    IN  p_email         VARCHAR(100),
    IN  p_password_hash VARCHAR(255),
    IN  p_role          VARCHAR(10),
    IN  p_performed_by  INT,
    OUT p_new_user_id   INT,
    OUT p_result_msg    VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_new_user_id = 0;
        SET p_result_msg  = 'ERROR: Username or email already exists.';
    END;

    START TRANSACTION;
    INSERT INTO USER (username, email, password_hash, role, account_status)
    VALUES (p_username, p_email, p_password_hash, p_role, 'ACTIVE');
    SET p_new_user_id = LAST_INSERT_ID();
    INSERT INTO USER_EVENT (target_user_id, performed_by, event_type)
    VALUES (p_new_user_id, p_performed_by, 'CREATED');
    COMMIT;
    SET p_result_msg = CONCAT('SUCCESS: User created with ID ', p_new_user_id);
END$$

-- PROCEDURE 2: Change password with full validation
CREATE PROCEDURE sp_change_password(
    IN  p_user_id    INT,
    IN  p_old_hash   VARCHAR(255),
    IN  p_new_hash   VARCHAR(255),
    IN  p_source_ip  VARCHAR(45),
    OUT p_success    TINYINT,
    OUT p_result_msg VARCHAR(255)
)
BEGIN
    DECLARE v_stored_hash VARCHAR(255);
    DECLARE v_status      VARCHAR(20);

    SELECT password_hash, account_status INTO v_stored_hash, v_status
    FROM USER WHERE user_id = p_user_id;

    IF v_stored_hash IS NULL THEN
        SET p_success = 0; SET p_result_msg = 'ERROR: User not found.';
    ELSEIF v_status <> 'ACTIVE' THEN
        SET p_success = 0;
        SET p_result_msg = CONCAT('ERROR: Account is ', v_status, '. Cannot change password.');
    ELSEIF v_stored_hash <> p_old_hash THEN
        INSERT INTO PASSWORD_EVENT (user_id, event_type, event_status) VALUES (p_user_id, 'CHANGE', 'FAILURE');
        INSERT INTO INPUT_VALIDATION_LOG (user_id, input_field, failure_reason, source_ip)
        VALUES (p_user_id, 'password', 'Old password mismatch during change', p_source_ip);
        SET p_success = 0; SET p_result_msg = 'ERROR: Current password is incorrect.';
    ELSEIF p_old_hash = p_new_hash THEN
        SET p_success = 0; SET p_result_msg = 'ERROR: New password must differ from current.';
    ELSE
        UPDATE USER SET password_hash = p_new_hash WHERE user_id = p_user_id;
        INSERT INTO PASSWORD_EVENT (user_id, event_type, event_status) VALUES (p_user_id, 'CHANGE', 'SUCCESS');
        SET p_success = 1; SET p_result_msg = 'SUCCESS: Password changed successfully.';
    END IF;
END$$

-- PROCEDURE 3: Full user audit report (5 result sets)
CREATE PROCEDURE sp_get_user_audit_report(IN p_user_id INT)
BEGIN
    SELECT user_id, username, email, role, account_status, created_at
    FROM USER WHERE user_id = p_user_id;

    SELECT auth_id, auth_type, auth_result, source_ip, timestamp
    FROM AUTHENTICATION WHERE user_id = p_user_id ORDER BY timestamp DESC LIMIT 20;

    SELECT password_event_id, event_type, event_status, timestamp
    FROM PASSWORD_EVENT WHERE user_id = p_user_id ORDER BY timestamp DESC;

    SELECT session_id, login_time, logout_time, session_status, source_ip
    FROM SESSION WHERE user_id = p_user_id ORDER BY login_time DESC;

    SELECT validation_log_id, input_field, failure_reason, source_ip, timestamp
    FROM INPUT_VALIDATION_LOG WHERE user_id = p_user_id ORDER BY timestamp DESC;
END$$

-- PROCEDURE 4: Bulk expire old sessions
CREATE PROCEDURE sp_terminate_expired_sessions(
    IN  p_hours         INT,
    OUT p_expired_count INT
)
BEGIN
    UPDATE SESSION SET session_status = 'EXPIRED', logout_time = NOW()
    WHERE session_status = 'ACTIVE' AND login_time < (NOW() - INTERVAL p_hours HOUR);
    SET p_expired_count = ROW_COUNT();
END$$

-- PROCEDURE 5: Log login attempt + return user data in one call
CREATE PROCEDURE sp_login_attempt(
    IN  p_username    VARCHAR(50),
    IN  p_auth_result VARCHAR(10),
    IN  p_source_ip   VARCHAR(45),
    OUT p_user_id     INT,
    OUT p_role        VARCHAR(10),
    OUT p_status      VARCHAR(15)
)
BEGIN
    DECLARE v_user_id INT DEFAULT NULL;
    DECLARE v_role    VARCHAR(10);
    DECLARE v_status  VARCHAR(15);

    SELECT user_id, role, account_status INTO v_user_id, v_role, v_status
    FROM USER WHERE username = p_username;

    INSERT INTO AUTHENTICATION (user_id, auth_type, auth_result, source_ip)
    VALUES (v_user_id, 'LOGIN', p_auth_result, p_source_ip);

    SET p_user_id = v_user_id;
    SET p_role    = v_role;
    SET p_status  = v_status;
END$$

DELIMITER ;

-- ============================================================
-- SECTION 4: SEED DATA
-- ============================================================
INSERT IGNORE INTO USER (username, email, password_hash, role, account_status)
VALUES ('admin', 'admin@audit.local',
        'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7',
        'ADMIN', 'ACTIVE');

