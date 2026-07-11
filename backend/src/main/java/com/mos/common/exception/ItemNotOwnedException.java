package com.mos.common.exception;

public class ItemNotOwnedException extends BusinessException {

    public ItemNotOwnedException() {
        super("You do not own this item");
    }
}
