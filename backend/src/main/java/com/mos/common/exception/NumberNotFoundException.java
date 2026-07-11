package com.mos.common.exception;

public class NumberNotFoundException extends BusinessException {

    public NumberNotFoundException() {
        super("Number not found");
    }
}
