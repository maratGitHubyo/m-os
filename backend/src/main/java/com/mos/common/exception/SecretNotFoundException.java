package com.mos.common.exception;

public class SecretNotFoundException extends BusinessException {

    public SecretNotFoundException() {
        super("Secret code not found");
    }
}
