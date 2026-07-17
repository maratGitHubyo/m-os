package com.mos.common.exception;

public class LoreFragmentAlreadyFoundException extends BusinessException {

    public LoreFragmentAlreadyFoundException() {
        super("This lore fragment has already been found");
    }
}
