package com.campuscafe.backend.exception;

public class InventoryCategoryNotFoundException extends RuntimeException {
    public InventoryCategoryNotFoundException(String message) {
        super(message);
    }
}
