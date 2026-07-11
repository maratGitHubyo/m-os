package com.mos.common.exception;

public class QrScanLimitReachedException extends BusinessException {

    public QrScanLimitReachedException() {
        super("QR code scan limit reached");
    }
}
