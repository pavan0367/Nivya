-- ==============================================================================
-- V16__account_deletion_approval_codes.sql
-- Child Account Deletion Parent Approval Codes Table
-- ==============================================================================

CREATE TABLE IF NOT EXISTS deletion_approval_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    child_user_id BIGINT NOT NULL,
    parent_user_id BIGINT NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_del_code_child FOREIGN KEY (child_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_del_code_parent FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_del_code_child_status ON deletion_approval_codes(child_user_id, status);
CREATE INDEX idx_del_code_hash ON deletion_approval_codes(code_hash);
CREATE INDEX idx_del_code_expires ON deletion_approval_codes(expires_at);
