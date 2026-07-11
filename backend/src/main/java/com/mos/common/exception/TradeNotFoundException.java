package com.mos.common.exception;

public class TradeNotFoundException extends BusinessException {

    public TradeNotFoundException() {
        super("Trade not found");
    }
}
