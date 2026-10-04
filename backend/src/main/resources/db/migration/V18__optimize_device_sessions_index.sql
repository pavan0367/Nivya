-- ==============================================================================
-- V18__optimize_device_sessions_index.sql
-- Composite indexes for high-throughput login session verification and count lookups
-- ==============================================================================

CREATE INDEX idx_sessions_user_fingerprint ON authenticated_device_sessions(user_id, device_fingerprint);
CREATE INDEX idx_sessions_user_fp_status ON authenticated_device_sessions(user_id, device_fingerprint, status);
