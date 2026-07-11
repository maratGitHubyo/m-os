package com.mos.victory.dto;

public record VictoryProgress(
        long current,
        long target,
        String label,
        boolean satisfied
) {

    public static VictoryProgress of(long current, long target, String label) {
        return new VictoryProgress(current, target, label, current >= target && target > 0);
    }

    public static VictoryProgress notApplicable(String label) {
        return new VictoryProgress(0, 0, label, false);
    }

    public static VictoryProgress stub(long current, long target, String label) {
        return new VictoryProgress(current, target, label, false);
    }
}
