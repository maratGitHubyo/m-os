package com.mos.common.exception;

public class ConcurrentModificationException extends BusinessException {

    public ConcurrentModificationException() {
        super("Wallet was modified concurrently, please retry");
    }
}
