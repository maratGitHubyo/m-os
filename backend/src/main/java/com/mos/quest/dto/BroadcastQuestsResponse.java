package com.mos.quest.dto;

public record BroadcastQuestsResponse(
        int playerCount,
        int questsCreated,
        int countPerPlayer
) {
}
