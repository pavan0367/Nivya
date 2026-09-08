-- ==============================================================================
-- V6__init_location.sql
-- Nivya Location Status, Consented Location History, and Time-Based Indexes
-- ==============================================================================

-- 1. Location Status Table (Current / Last Known Location snapshot per device)
CREATE TABLE IF NOT EXISTS location_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL UNIQUE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy_meters FLOAT NULL,
    altitude_meters DOUBLE PRECISION NULL,
    speed_meters_per_sec FLOAT NULL,
    bearing_degrees FLOAT NULL,
    provider VARCHAR(20) NOT NULL DEFAULT 'gps',
    is_gps_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_network_available BOOLEAN NOT NULL DEFAULT TRUE,
    permission_state VARCHAR(30) NOT NULL DEFAULT 'GRANTED',
    is_background_consented BOOLEAN NOT NULL DEFAULT FALSE,
    is_stale BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_location_status_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_location_status_device ON location_status(device_id);
CREATE INDEX idx_location_status_recorded ON location_status(recorded_at);

-- 2. Location History Table (Chronological location breadcrumbs for consented tracking)
CREATE TABLE IF NOT EXISTS location_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy_meters FLOAT NULL,
    altitude_meters DOUBLE PRECISION NULL,
    speed_meters_per_sec FLOAT NULL,
    provider VARCHAR(20) NOT NULL DEFAULT 'gps',
    source_mode VARCHAR(20) NOT NULL DEFAULT 'FOREGROUND',
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_location_history_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_location_history_device_time ON location_history(device_id, recorded_at);
CREATE INDEX idx_location_history_recorded ON location_history(recorded_at);
