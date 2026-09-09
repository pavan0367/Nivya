-- ==============================================================================
-- V11__init_convocation.sql
-- Nivya Convocation: Priority Family Communication & Ephemeral Child Viewing
-- ==============================================================================

-- 1. Convocation Messages Table
CREATE TABLE IF NOT EXISTS convocation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    receiver_user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    seen_at TIMESTAMP NULL,
    view_started_at TIMESTAMP NULL,
    visibility_expires_at TIMESTAMP NULL,
    child_visibility_expires_at TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UNREAD',
    CONSTRAINT fk_convocation_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_convocation_sender FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_convocation_receiver FOREIGN KEY (receiver_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_conv_family_created ON convocation_messages(family_id, created_at DESC);
CREATE INDEX idx_conv_receiver_status ON convocation_messages(receiver_user_id, status);
CREATE INDEX idx_conv_visibility ON convocation_messages(receiver_user_id, visibility_expires_at);
CREATE INDEX idx_conv_child_expires ON convocation_messages(receiver_user_id, child_visibility_expires_at);

-- 2. Convocation Viewing Sessions Table (Authoritative 2-minute sessions)
CREATE TABLE IF NOT EXISTS convocation_views (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    session_uuid VARCHAR(64) NOT NULL UNIQUE,
    view_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    visibility_expires_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conv_views_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_views_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_conv_view_user ON convocation_views(user_id, status);
CREATE INDEX idx_conv_view_expiry ON convocation_views(visibility_expires_at);
