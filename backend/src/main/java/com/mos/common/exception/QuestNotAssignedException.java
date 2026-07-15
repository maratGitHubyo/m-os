package com.mos.common.exception;

public class QuestNotAssignedException extends BusinessException {

    public QuestNotAssignedException() {
        super("Quest is not assigned to you");
    }
}
