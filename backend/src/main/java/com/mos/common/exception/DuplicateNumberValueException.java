package com.mos.common.exception;

public class DuplicateNumberValueException extends BusinessException {

    public DuplicateNumberValueException() {
        super("Number value already exists in this game session");
    }
}
