package com.mos.common.exception;

public class AuctionBidTooLowException extends BusinessException {

    public AuctionBidTooLowException() {
        super("Bid amount is too low");
    }
}
