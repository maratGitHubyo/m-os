package com.mos.common.exception;

public class UniqueItemAlreadyExistsException extends BusinessException {

    public UniqueItemAlreadyExistsException() {
        super("Unique item already exists in this game session");
    }
}
