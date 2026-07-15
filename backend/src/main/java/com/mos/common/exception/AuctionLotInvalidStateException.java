package com.mos.common.exception;

public class AuctionLotInvalidStateException extends BusinessException {

    public AuctionLotInvalidStateException() {
        super("Auction lot is not in a valid state for this operation");
    }
}
