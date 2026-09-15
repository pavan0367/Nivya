-- ==============================================================================
-- V15__convocation_actions.sql
-- Add pin, reaction, and reply context to convocation messages
-- ==============================================================================

ALTER TABLE convocation_messages ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE convocation_messages ADD COLUMN reaction VARCHAR(64) NULL;
ALTER TABLE convocation_messages ADD COLUMN reply_to_id BIGINT NULL;
