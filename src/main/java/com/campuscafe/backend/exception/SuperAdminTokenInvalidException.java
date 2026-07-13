package com.campuscafe.backend.exception;

/**
 * Exception thrown when the Super Admin verification token is invalid, expired, or has already been used.
 */
public class SuperAdminTokenInvalidException extends RuntimeException {
    public SuperAdminTokenInvalidException(String message) {
        super(message);
    }
}
