package com.mos.common.exception;

public class QrAlreadyScannedException extends BusinessException {

    public QrAlreadyScannedException() {
        super("You have already scanned this QR code");
    }
}
