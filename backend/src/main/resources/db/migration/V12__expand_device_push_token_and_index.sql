-- ============================================================================
-- NIVYA DATABASE MIGRATION: V12__expand_device_push_token_and_index.sql
-- Phase 19: Push Notifications & Device Token Management
-- ============================================================================

-- Expand push_token column to support full-length Firebase Cloud Messaging (FCM) registration tokens
ALTER TABLE devices MODIFY push_token VARCHAR(1024) NULL;

-- Create index for fast push token lookups, replacement, and revocation
-- Prefix length 768 characters is the maximum allowed under MySQL InnoDB's 3072-byte key limit with utf8mb4 (768 * 4 = 3072 bytes)
CREATE INDEX idx_devices_push_token ON devices(push_token(768));
