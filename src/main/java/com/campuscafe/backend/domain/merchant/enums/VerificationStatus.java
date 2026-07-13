package com.campuscafe.backend.domain.merchant.enums;

/**
 * Represents the onboarding verification status of a merchant.
 */
public enum VerificationStatus {
    /**
     * Merchant has registered and verified email, awaiting Super Admin approval.
     */
    PENDING,

    /**
     * Merchant is approved and allowed to log in and manage the cafe.
     */
    VERIFIED,

    /**
     * Merchant registration has been rejected by the Super Admin.
     */
    REJECTED
}
