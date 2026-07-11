package com.mos.common.exception;

public class CoinTransferSelfException extends BusinessException {

    public CoinTransferSelfException() {
        super("Cannot transfer coins to yourself");
    }
}
