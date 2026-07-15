package com.mos.common.exception;

public class QuestNotStartedException extends BusinessException {

    public QuestNotStartedException() {
        super("Quest is not started");
    }
}
