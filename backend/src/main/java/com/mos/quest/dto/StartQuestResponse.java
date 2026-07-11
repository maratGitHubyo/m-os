package com.mos.quest.dto;

public record StartQuestResponse(
        PlayerQuestResponse playerQuest
) {

    public static StartQuestResponse from(PlayerQuestResponse playerQuest) {
        return new StartQuestResponse(playerQuest);
    }
}
