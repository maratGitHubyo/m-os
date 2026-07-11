package com.mos.secret.dto;

import com.mos.item.dto.PlayerItemResponse;
import com.mos.secret.enums.SecretRewardType;

import java.util.UUID;

public record SecretRedeemResponse(
        UUID secretId,
        String code,
        String title,
        SecretRewardType rewardType,
        Long coinAmount,
        PlayerItemResponse grantedItem
) {
}
