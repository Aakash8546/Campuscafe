package com.campuscafe.backend.exception;

/**
 * Exception thrown when a requested merchant account does not exist in the database.
 */
public class MerchantNotFoundException extends RuntimeException {
    public MerchantNotFoundException(String message) {
        super(message);
    }
}
