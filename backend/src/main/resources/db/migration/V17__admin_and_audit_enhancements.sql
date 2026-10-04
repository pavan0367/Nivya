-- ==============================================================================
-- V17__admin_and_audit_enhancements.sql
-- Administrative Management and Extended Audit Logging
-- ==============================================================================

-- 1. Add target_user_id to audit_logs table for administrative tracking
ALTER TABLE audit_logs ADD COLUMN target_user_id BIGINT NULL;
CREATE INDEX idx_audit_logs_target_user ON audit_logs(target_user_id);
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_target_user FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE SET NULL;
