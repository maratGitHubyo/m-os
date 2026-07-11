package com.mos.common.exception;

public class ItemSessionMismatchException extends BusinessException {

    public ItemSessionMismatchException() {
        super("Item belongs to a different game session");
    }
}
