-- ==============================================================================
-- V2__init_families_and_pairing.sql
-- Nivya Family, Device, Status, Pairing, Consent, and Audit Persistence
-- ==============================================================================

-- 1. Families Table
CREATE TABLE IF NOT EXISTS families (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_families_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_families_code ON families(family_code);
CREATE INDEX idx_families_created_by ON families(created_by);

-- 2. Family Members (Links users into families with explicit roles)
CREATE TABLE IF NOT EXISTS family_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_family_user UNIQUE (family_id, user_id),
    CONSTRAINT fk_family_members_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_family_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_family_members_user ON family_members(user_id);
CREATE INDEX idx_family_members_family ON family_members(family_id);

-- 3. Devices Table
CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_uuid VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    family_id BIGINT NULL,
    device_name VARCHAR(100) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    os_version VARCHAR(50) NULL,
    app_version VARCHAR(50) NULL,
    push_token VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_seen_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_devices_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE SET NULL
);

CREATE INDEX idx_devices_uuid ON devices(device_uuid);
CREATE INDEX idx_devices_user ON devices(user_id);
CREATE INDEX idx_devices_family ON devices(family_id);

-- 4. Device Status Table (Online/Offline telemetry & Stale state indicator)
CREATE TABLE IF NOT EXISTS device_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL UNIQUE,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    battery_pct INT NULL,
    network_type VARCHAR(30) NULL,
    network_quality VARCHAR(30) NULL,
    last_sync_at TIMESTAMP NULL,
    CONSTRAINT fk_device_status_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_device_status_device ON device_status(device_id);
CREATE INDEX idx_device_status_online ON device_status(is_online);

-- 5. Pairing Requests (Time-limited connection tokens with 1-time consumption)
CREATE TABLE IF NOT EXISTS pairing_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_user_id BIGINT NOT NULL,
    connection_code VARCHAR(32) NOT NULL UNIQUE,
    target_role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    device_fingerprint VARCHAR(100) NULL,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP NULL,
    accepted_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pairing_requester FOREIGN KEY (requester_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_pairing_acceptor FOREIGN KEY (accepted_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_pairing_code ON pairing_requests(connection_code);
CREATE INDEX idx_pairing_requester ON pairing_requests(requester_user_id);
CREATE INDEX idx_pairing_status ON pairing_requests(status);
CREATE INDEX idx_pairing_expires ON pairing_requests(expires_at);

-- 6. Consents Table (Cryptographically auditable consent records)
CREATE TABLE IF NOT EXISTS consents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    family_id BIGINT NOT NULL,
    terms_version VARCHAR(20) NOT NULL,
    monitoring_consent BOOLEAN NOT NULL DEFAULT TRUE,
    location_consent BOOLEAN NOT NULL DEFAULT TRUE,
    accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_consents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_consents_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

CREATE INDEX idx_consents_user ON consents(user_id);
CREATE INDEX idx_consents_family ON consents(family_id);

-- 7. Audit Logs Table (Immutable security and pairing audit events)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT NULL,
    ip_address VARCHAR(45) NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
