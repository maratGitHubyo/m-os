package com.mos.common.exception;

public class InsufficientScoreException extends BusinessException {

    public InsufficientScoreException() {
        super("Insufficient score points");
    }
}
