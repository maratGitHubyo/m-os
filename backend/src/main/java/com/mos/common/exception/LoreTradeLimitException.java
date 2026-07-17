package com.mos.common.exception;

public class LoreTradeLimitException extends BusinessException {

    public LoreTradeLimitException() {
        super("A player can hold only one lore fragment at a time");
    }
}
