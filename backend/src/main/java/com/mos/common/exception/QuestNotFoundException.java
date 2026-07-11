package com.mos.common.exception;

public class QuestNotFoundException extends BusinessException {

    public QuestNotFoundException() {
        super("Quest not found");
    }
}
