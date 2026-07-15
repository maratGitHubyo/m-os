package com.mos.common.exception;

public class AuctionModeInactiveException extends BusinessException {

    public AuctionModeInactiveException() {
        super("Auction is not open yet");
    }
}
