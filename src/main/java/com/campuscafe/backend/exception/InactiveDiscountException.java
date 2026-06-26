package com.campuscafe.backend.exception;

public class InactiveDiscountException extends RuntimeException {
    public InactiveDiscountException(String message) {
        super(message);
    }
}
