-- ==============================================================================
-- V10__init_history_events.sql
-- Nivya Parent-only History
-- Stores chronological activity timeline, application labels, contact/document
-- contexts, and active durations within consented scope.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS history_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    package_name VARCHAR(150) NOT NULL,
    app_name VARCHAR(150) NOT NULL,
    broad_activity VARCHAR(255) NOT NULL,
    activity_label VARCHAR(255) NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    duration_seconds INT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_events_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_history_device_timestamp ON history_events(device_id, event_timestamp DESC);
CREATE INDEX idx_history_device_app ON history_events(device_id, app_name);
CREATE INDEX idx_history_device_pkg ON history_events(device_id, package_name);
CREATE INDEX idx_history_device_date ON history_events(device_id, event_timestamp);
