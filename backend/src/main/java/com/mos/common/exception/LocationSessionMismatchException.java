package com.mos.common.exception;

public class LocationSessionMismatchException extends BusinessException {

    public LocationSessionMismatchException() {
        super("Location belongs to a different game session");
    }
}
