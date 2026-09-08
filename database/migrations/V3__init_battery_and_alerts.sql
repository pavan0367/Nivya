-- ==============================================================================
-- V3__init_battery_and_alerts.sql
-- Nivya Battery Telemetry, Historical Trends, and Safety Alerts Persistence
-- ==============================================================================

-- 1. Battery Status Table (Latest snapshot per device)
CREATE TABLE IF NOT EXISTS battery_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL UNIQUE,
    battery_pct INT NOT NULL,
    charging_state VARCHAR(30) NOT NULL DEFAULT 'DISCHARGING',
    battery_state VARCHAR(30) NOT NULL DEFAULT 'UNPLUGGED',
    health VARCHAR(30) NOT NULL DEFAULT 'GOOD',
    temperature_celsius DOUBLE NULL,
    is_low_battery BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_battery_status_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_battery_status_device ON battery_status(device_id);
CREATE INDEX idx_battery_status_low ON battery_status(is_low_battery);

-- 2. Battery History Table (Chronological telemetry points for trend analysis)
CREATE TABLE IF NOT EXISTS battery_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    battery_pct INT NOT NULL,
    charging_state VARCHAR(30) NOT NULL,
    battery_state VARCHAR(30) NOT NULL,
    health VARCHAR(30) NOT NULL,
    temperature_celsius DOUBLE NULL,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_battery_history_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_battery_history_device_time ON battery_history(device_id, recorded_at);
CREATE INDEX idx_battery_history_recorded ON battery_history(recorded_at);

-- 3. Alerts Table (Persistent safety alerts, e.g. Low Battery alerts)
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alerts_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_alerts_family ON alerts(family_id);
CREATE INDEX idx_alerts_device ON alerts(device_id);
CREATE INDEX idx_alerts_resolved ON alerts(resolved);
CREATE INDEX idx_alerts_type ON alerts(alert_type);
