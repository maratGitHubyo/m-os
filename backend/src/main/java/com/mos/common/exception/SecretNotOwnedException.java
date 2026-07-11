package com.mos.common.exception;

public class SecretNotOwnedException extends BusinessException {

    public SecretNotOwnedException() {
        super("This secret is not assigned to you");
    }
}
