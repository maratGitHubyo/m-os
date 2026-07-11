package com.mos.common.exception;

public class QuestAlreadyCompletedException extends BusinessException {

    public QuestAlreadyCompletedException() {
        super("Quest already completed");
    }
}
