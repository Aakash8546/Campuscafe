# Super Admin Merchant Onboarding & Verification Flow

This document details the merchant signup validation and verification workflow implemented in the Campus Cafe POS backend.

## Process Flow

1. **Signup Phase**
   * The owner submits registration details (including Full Name, Address, City, Pincode, Confirm Password).
   * A merchant profile is created in a `PENDING` state with `email_verified` set to `false`.
   * A random OTP is generated for email verification, and a secure UUID token is generated for Super Admin action.

2. **Email Verification**
   * The owner enters the OTP received via email to verify their email address.
   * On successful verification, the merchant's `email_verified` status is updated to `true`.
   * At this stage, the merchant's verification status remains `PENDING`. They are not allowed to log in yet.

3. **Super Admin Approval**
   * Upon signup, an email is dispatched to the Super Admin (`aakashsrivastava2151@gmail.com`) containing the registration details and verification action buttons.
   * Clicking "Approve" triggers:
     `GET /auth/super-admin/merchants/verify?merchantId={id}&token={token}&action=VERIFIED`
   * The status is updated to `VERIFIED` and the `superAdminToken` is cleared (nullified) to prevent replay attacks.
   * The merchant is now active and can successfully log in to the POS.
