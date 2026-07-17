package com.mos.common.exception;

public class PlayerAlreadyHasLoreItemException extends BusinessException {

    public PlayerAlreadyHasLoreItemException() {
        super("You already have a lore fragment. Sell, trade, or give it to another player to take a new one.");
    }
}
