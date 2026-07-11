package com.mos.common.exception;

public class GameEventNotFoundException extends BusinessException {

    public GameEventNotFoundException() {
        super("Game event not found");
    }
}
