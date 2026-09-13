-- ==============================================================================
-- V14__expand_pairing_device_fingerprint.sql
-- Expand device_fingerprint columns in pairing and session tables
-- to accommodate modern browser User-Agent strings and client device identifiers
-- ==============================================================================

ALTER TABLE pairing_requests MODIFY device_fingerprint VARCHAR(255) NULL;
ALTER TABLE authenticated_device_sessions MODIFY device_fingerprint VARCHAR(255) NOT NULL;
