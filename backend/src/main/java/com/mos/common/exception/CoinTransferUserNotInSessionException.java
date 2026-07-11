package com.mos.common.exception;

public class CoinTransferUserNotInSessionException extends BusinessException {

    public CoinTransferUserNotInSessionException() {
        super("User is not a participant of this game session");
    }
}
