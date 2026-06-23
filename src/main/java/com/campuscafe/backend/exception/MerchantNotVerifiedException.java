package com.campuscafe.backend.exception;

public class MerchantNotVerifiedException extends RuntimeException {
    public MerchantNotVerifiedException(String message) {
        super(message);
    }
}
