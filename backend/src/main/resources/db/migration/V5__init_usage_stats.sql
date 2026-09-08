-- ==============================================================================
-- V5__init_usage_stats.sql
-- Nivya Screen Time and Application Usage Statistics Persistence
-- ==============================================================================

-- 1. Daily Usage Summary Table (Daily totals and category aggregates per device)
CREATE TABLE IF NOT EXISTS usage_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    date DATE NOT NULL,
    total_foreground_seconds BIGINT NOT NULL DEFAULT 0,
    screen_unlocks INT NOT NULL DEFAULT 0,
    educational_seconds BIGINT NOT NULL DEFAULT 0,
    recreational_seconds BIGINT NOT NULL DEFAULT 0,
    social_seconds BIGINT NOT NULL DEFAULT 0,
    productivity_seconds BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_usage_summary_device_date UNIQUE (device_id, date),
    CONSTRAINT fk_usage_summary_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_usage_summary_device ON usage_summary(device_id);
CREATE INDEX idx_usage_summary_date ON usage_summary(date);

-- 2. App Usage Details Table (Per-application foreground durations and categories)
CREATE TABLE IF NOT EXISTS usage_apps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    summary_id BIGINT NOT NULL,
    package_name VARCHAR(150) NOT NULL,
    app_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    foreground_seconds BIGINT NOT NULL DEFAULT 0,
    last_time_used TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usage_apps_summary_pkg UNIQUE (summary_id, package_name),
    CONSTRAINT fk_usage_apps_summary FOREIGN KEY (summary_id) REFERENCES usage_summary(id) ON DELETE CASCADE
);

CREATE INDEX idx_usage_apps_summary ON usage_apps(summary_id);
CREATE INDEX idx_usage_apps_category ON usage_apps(category);
