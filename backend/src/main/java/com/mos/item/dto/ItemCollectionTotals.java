package com.mos.item.dto;

public record ItemCollectionTotals(
        long common,
        long rare,
        long epic,
        long legendary,
        long total,
        long unclaimed
) {
}
