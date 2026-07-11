package com.mos.wallet.transfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCoinTransferRequest(
        @NotNull UUID receiverUserId,
        @NotNull @Min(1) Long amount
) {
}
