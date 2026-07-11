package com.mos.common.exception;

public class QuestNotActiveException extends BusinessException {

    public QuestNotActiveException() {
        super("Quest is not active");
    }
}
