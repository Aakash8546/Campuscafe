package com.campuscafe.backend.exception;

public class DuplicateInventoryCategoryException extends RuntimeException {
    public DuplicateInventoryCategoryException(String message) {
        super(message);
    }
}
