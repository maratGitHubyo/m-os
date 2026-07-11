package com.mos.common.exception;

public class TradeInsufficientCoinsException extends BusinessException {

    public TradeInsufficientCoinsException() {
        super("Insufficient coins for this trade");
    }
}
