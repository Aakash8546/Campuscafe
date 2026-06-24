package com.campuscafe.backend.exception;

public class DuplicateInventoryItemException extends RuntimeException {
    public DuplicateInventoryItemException(String message) {
        super(message);
    }
}
