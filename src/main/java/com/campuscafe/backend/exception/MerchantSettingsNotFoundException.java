package com.campuscafe.backend.exception;

public class MerchantSettingsNotFoundException extends RuntimeException {
    public MerchantSettingsNotFoundException(String message) {
        super(message);
    }
}
