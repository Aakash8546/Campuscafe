package com.campuscafe.backend.exception;

public class DuplicateDiscountException extends RuntimeException {
    public DuplicateDiscountException(String message) {
        super(message);
    }
}
