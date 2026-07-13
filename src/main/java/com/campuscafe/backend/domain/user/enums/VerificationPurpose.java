package com.campuscafe.backend.domain.user.enums;

/**
 * Purpose description of a generated OTP verification token.
 */
public enum VerificationPurpose {
    /**
     * Sent to user email to verify ownership of the account email address.
     */
    EMAIL_VERIFICATION,

    /**
     * Sent to user email to authorize password reset requests.
     */
    PASSWORD_RESET
}
