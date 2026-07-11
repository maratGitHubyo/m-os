package com.mos.common.exception;

public class InvalidAmountException extends BusinessException {

    public InvalidAmountException() {
        super("Amount must be greater than zero");
    }
}
