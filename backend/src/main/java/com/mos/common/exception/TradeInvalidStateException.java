package com.mos.common.exception;

public class TradeInvalidStateException extends BusinessException {

    public TradeInvalidStateException() {
        super("Trade is not in a valid state for this operation");
    }
}
