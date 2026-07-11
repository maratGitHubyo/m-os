package com.mos.common.exception;

public class TradeNotParticipantException extends BusinessException {

    public TradeNotParticipantException() {
        super("You are not a participant of this trade");
    }
}
