package com.mos.common.exception;

public class UserInactiveException extends BusinessException {

    public UserInactiveException() {
        super("User account is inactive");
    }
}
