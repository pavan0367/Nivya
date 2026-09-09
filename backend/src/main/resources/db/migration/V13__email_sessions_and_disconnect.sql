-- ==============================================================================
-- V13__email_sessions_and_disconnect.sql
-- Email Verification, Notifications, Preferences, Device Sessions & Disconnect Codes
-- ==============================================================================

-- 1. Email Verification Codes (Secure, single-use, time-limited, rate-limited)
CREATE TABLE IF NOT EXISTS email_verification_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    email VARCHAR(191) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL DEFAULT 'EMAIL_VERIFICATION',
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_email ON email_verification_codes(email, used_at);
CREATE INDEX idx_email_verification_hash ON email_verification_codes(code_hash);
CREATE INDEX idx_email_verification_expires ON email_verification_codes(expires_at);

-- 2. Email Preferences (Per-user configurable email settings)
CREATE TABLE IF NOT EXISTS email_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    login_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    new_device_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    app_updates_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    security_critical_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_pref_user ON email_preferences(user_id);

-- 3. Email Notifications & Delivery Tracking (Audited delivery status, retries, idempotency)
CREATE TABLE IF NOT EXISTS email_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    recipient_email VARCHAR(191) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'SIMULATION',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    idempotency_key VARCHAR(100) NULL UNIQUE,
    provider_message_id VARCHAR(255) NULL,
    failure_reason TEXT NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_email_notif_recipient ON email_notifications(recipient_email);
CREATE INDEX idx_email_notif_status ON email_notifications(status);
CREATE INDEX idx_email_notif_idempotency ON email_notifications(idempotency_key);
CREATE INDEX idx_email_notif_created ON email_notifications(created_at);

-- 4. Authenticated Device Sessions (Active session tracking, platform, approximate location)
CREATE TABLE IF NOT EXISTS authenticated_device_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_fingerprint VARCHAR(128) NOT NULL,
    device_name VARCHAR(100) NULL,
    platform VARCHAR(30) NULL,
    os_version VARCHAR(50) NULL,
    app_version VARCHAR(50) NULL,
    ip_address VARCHAR(45) NULL,
    approximate_location VARCHAR(150) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_at TIMESTAMP NULL,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_sessions_user_status ON authenticated_device_sessions(user_id, status);
CREATE INDEX idx_sessions_fingerprint ON authenticated_device_sessions(device_fingerprint);
CREATE INDEX idx_sessions_last_activity ON authenticated_device_sessions(last_activity_at);

-- 5. Disconnect Codes (Parent-generated cryptographically secure one-time disconnect codes)
CREATE TABLE IF NOT EXISTS disconnect_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    parent_user_id BIGINT NOT NULL,
    child_user_id BIGINT NULL,
    code_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_disconnect_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_disconnect_parent FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_disconnect_child FOREIGN KEY (child_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_disconnect_family_status ON disconnect_codes(family_id, status);
CREATE INDEX idx_disconnect_code_hash ON disconnect_codes(code_hash);
CREATE INDEX idx_disconnect_expires ON disconnect_codes(expires_at);
