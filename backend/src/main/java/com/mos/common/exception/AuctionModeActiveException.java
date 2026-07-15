package com.mos.common.exception;

public class AuctionModeActiveException extends BusinessException {

    public AuctionModeActiveException() {
        super("Transfers and trades are disabled while the auction is active");
    }
}
