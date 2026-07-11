package com.mos.common.exception;

public class TradeItemNotOwnedException extends BusinessException {

    public TradeItemNotOwnedException() {
        super("Trade item is not owned by the expected player");
    }
}
