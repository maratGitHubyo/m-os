package com.mos.common.exception;

public class AuctionNoBidsException extends BusinessException {

    public AuctionNoBidsException() {
        super("Cannot sell a lot with no bids");
    }
}
