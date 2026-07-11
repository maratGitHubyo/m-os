package com.mos.common.exception;

public class QrCodeNotFoundException extends BusinessException {

    public QrCodeNotFoundException() {
        super("QR code not found");
    }
}
