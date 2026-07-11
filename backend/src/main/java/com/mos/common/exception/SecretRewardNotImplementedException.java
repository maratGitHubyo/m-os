package com.mos.common.exception;

public class SecretRewardNotImplementedException extends BusinessException {

    public SecretRewardNotImplementedException(String rewardType) {
        super("Reward type not yet implemented: " + rewardType);
    }
}
