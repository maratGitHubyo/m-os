package com.mos.common.exception;

public class NumberSessionMismatchException extends BusinessException {

    public NumberSessionMismatchException() {
        super("Number does not belong to this game session");
    }
}
