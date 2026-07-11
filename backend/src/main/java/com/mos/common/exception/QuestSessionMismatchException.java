package com.mos.common.exception;

public class QuestSessionMismatchException extends BusinessException {

    public QuestSessionMismatchException() {
        super("Quest does not belong to this game session");
    }
}
