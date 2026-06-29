-- V24: Production hardening migrations
-- 1. Increase refresh_tokens.token column to TEXT to accommodate full JWT strings (350-450 chars)
ALTER TABLE refresh_tokens ALTER COLUMN token TYPE TEXT;

-- 2. Add composite index on verification_tokens(email, purpose) for OTP lookup performance
CREATE INDEX IF NOT EXISTS idx_verification_tokens_email_purpose
    ON verification_tokens (email, purpose);

-- 3. Add index for un-verified OTP invalidation queries
CREATE INDEX IF NOT EXISTS idx_verification_tokens_email_purpose_verified
    ON verification_tokens (email, purpose, verified);
