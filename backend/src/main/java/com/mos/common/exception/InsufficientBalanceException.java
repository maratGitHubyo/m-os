package com.mos.common.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException() {
        super("Insufficient M-coin balance");
    }
}
