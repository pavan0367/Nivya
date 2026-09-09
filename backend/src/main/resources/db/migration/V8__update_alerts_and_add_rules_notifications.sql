-- ==============================================================================
-- V8__update_alerts_and_add_rules_notifications.sql
-- Nivya Alerts Architecture: Rules, Event Read/Resolve State, Role Targeting,
-- and Dispatched Notification Receipts.
-- ==============================================================================

-- 1. Update Existing Alerts Table with Read Tracking and Role Segregation
ALTER TABLE alerts ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE alerts ADD COLUMN read_at TIMESTAMP NULL;
ALTER TABLE alerts ADD COLUMN target_role VARCHAR(20) NOT NULL DEFAULT 'PARENT';

CREATE INDEX idx_alerts_family_read ON alerts(family_id, is_read);
CREATE INDEX idx_alerts_target_role ON alerts(target_role);

-- 2. Alert Rules Table (Configurable safety and telemetry threshold policies)
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    threshold_value VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    target_role VARCHAR(20) NOT NULL DEFAULT 'PARENT',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_rules_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

CREATE INDEX idx_alert_rules_family ON alert_rules(family_id);
CREATE INDEX idx_alert_rules_type ON alert_rules(family_id, rule_type);

-- 3. Notifications Table (Dispatched notification records per user recipient)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP',
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'SENT',
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_notifications_alert FOREIGN KEY (alert_id) REFERENCES alerts(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_alert ON notifications(alert_id);
CREATE INDEX idx_notifications_status ON notifications(delivery_status);
