-- ==============================================================================
-- V9__init_activity_events.sql
-- Nivya Parent-only Live Activity Event Tracking
-- High-level broad activities (e.g. "Chatting with Arun", "Browsing", "Watching")
-- strictly restricted to Parent role with privacy safeguards.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS activity_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    package_name VARCHAR(150) NOT NULL,
    app_name VARCHAR(150) NOT NULL,
    broad_activity VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    duration_seconds INT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_device_time ON activity_events(device_id, started_at);
CREATE INDEX idx_activity_device_current ON activity_events(device_id, is_current);
