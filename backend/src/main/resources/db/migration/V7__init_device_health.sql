-- ==============================================================================
-- V7__init_device_health.sql
-- Nivya Device Health Telemetry, Hardware Diagnostics & Permission Auditing
-- ==============================================================================

CREATE TABLE IF NOT EXISTS device_health (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL UNIQUE,
    battery_pct INT NULL,
    charging_state VARCHAR(30) NULL,
    battery_health VARCHAR(30) NULL,
    battery_temp_celsius DOUBLE PRECISION NULL,
    storage_total_bytes BIGINT NULL,
    storage_used_bytes BIGINT NULL,
    storage_free_bytes BIGINT NULL,
    ram_total_bytes BIGINT NULL,
    ram_used_bytes BIGINT NULL,
    ram_free_bytes BIGINT NULL,
    is_low_ram BOOLEAN NOT NULL DEFAULT FALSE,
    device_model VARCHAR(100) NULL,
    device_manufacturer VARCHAR(100) NULL,
    os_version VARCHAR(50) NULL,
    sdk_version INT NULL,
    network_type VARCHAR(30) NULL,
    is_online BOOLEAN NOT NULL DEFAULT TRUE,
    location_permission VARCHAR(30) NOT NULL DEFAULT 'GRANTED',
    usage_permission VARCHAR(30) NOT NULL DEFAULT 'GRANTED',
    notification_permission VARCHAR(30) NOT NULL DEFAULT 'GRANTED',
    battery_optimization VARCHAR(30) NOT NULL DEFAULT 'OPTIMIZED',
    all_permissions_healthy BOOLEAN NOT NULL DEFAULT TRUE,
    health_score INT NOT NULL DEFAULT 100,
    health_status VARCHAR(30) NOT NULL DEFAULT 'HEALTHY',
    sync_state VARCHAR(30) NOT NULL DEFAULT 'SYNCED',
    recorded_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_health_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_device_health_device ON device_health(device_id);
CREATE INDEX idx_device_health_updated ON device_health(updated_at);
