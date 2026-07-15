package com.mos.common.exception;

public class AuctionLotNotFoundException extends BusinessException {

    public AuctionLotNotFoundException() {
        super("Auction lot not found");
    }
}
