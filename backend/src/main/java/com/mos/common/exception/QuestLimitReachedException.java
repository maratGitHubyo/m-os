package com.mos.common.exception;

public class QuestLimitReachedException extends BusinessException {

    public QuestLimitReachedException() {
        super("Quest completion limit reached");
    }
}
