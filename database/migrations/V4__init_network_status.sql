-- ==============================================================================
-- V4__init_network_status.sql
-- Nivya Network Telemetry, Connectivity Quality, and Historical Logging
-- ==============================================================================

-- 1. Network Status Table (Latest snapshot per device)
CREATE TABLE IF NOT EXISTS network_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL UNIQUE,
    network_type VARCHAR(30) NOT NULL DEFAULT 'NONE',
    connection_type VARCHAR(50) NULL,
    is_network_available BOOLEAN NOT NULL DEFAULT FALSE,
    is_internet_available BOOLEAN NOT NULL DEFAULT FALSE,
    signal_level INT NULL,
    signal_dbm INT NULL,
    quality VARCHAR(20) NOT NULL DEFAULT 'UNAVAILABLE',
    ssid VARCHAR(100) NULL,
    ip_address VARCHAR(45) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_network_status_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_network_status_device ON network_status(device_id);
CREATE INDEX idx_network_status_quality ON network_status(quality);
CREATE INDEX idx_network_status_internet ON network_status(is_internet_available);

-- 2. Network History Table (Chronological connectivity telemetry series)
CREATE TABLE IF NOT EXISTS network_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    network_type VARCHAR(30) NOT NULL,
    connection_type VARCHAR(50) NULL,
    is_network_available BOOLEAN NOT NULL,
    is_internet_available BOOLEAN NOT NULL,
    signal_level INT NULL,
    quality VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_network_history_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_network_history_device_time ON network_history(device_id, recorded_at);
CREATE INDEX idx_network_history_recorded ON network_history(recorded_at);
