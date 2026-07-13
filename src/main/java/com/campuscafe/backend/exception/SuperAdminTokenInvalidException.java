package com.campuscafe.backend.exception;

public class SuperAdminTokenInvalidException extends RuntimeException {
    public SuperAdminTokenInvalidException(String message) {
        super(message);
    }
}
