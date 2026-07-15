package com.mos.quest.dto;

import jakarta.validation.constraints.Size;

public record CompleteQuestRequest(
        @Size(max = 1000) String note
) {
}
