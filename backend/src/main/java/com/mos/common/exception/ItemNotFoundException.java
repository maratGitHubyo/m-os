package com.mos.common.exception;

public class ItemNotFoundException extends BusinessException {

    public ItemNotFoundException() {
        super("Item not found");
    }
}
