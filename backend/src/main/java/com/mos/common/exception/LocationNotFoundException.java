package com.mos.common.exception;

public class LocationNotFoundException extends BusinessException {

    public LocationNotFoundException() {
        super("Location not found");
    }
}
