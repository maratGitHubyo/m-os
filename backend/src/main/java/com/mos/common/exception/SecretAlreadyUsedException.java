package com.mos.common.exception;

public class SecretAlreadyUsedException extends BusinessException {

    public SecretAlreadyUsedException() {
        super("Secret code has already been used");
    }
}
