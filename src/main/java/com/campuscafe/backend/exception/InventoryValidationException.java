package com.campuscafe.backend.exception;

public class InventoryValidationException extends RuntimeException {
    public InventoryValidationException(String message) {
        super(message);
    }
}
