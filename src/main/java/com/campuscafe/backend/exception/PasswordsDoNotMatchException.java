package com.campuscafe.backend.exception;

/**
 * Exception thrown when the password and confirm password fields do not match during signup.
 */
public class PasswordsDoNotMatchException extends RuntimeException {
    public PasswordsDoNotMatchException(String message) {
        super(message);
    }
}
