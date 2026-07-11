package com.mos.quest.controller;

import com.mos.quest.dto.CreateQuestRequest;
import com.mos.quest.dto.QuestResponse;
import com.mos.quest.dto.UpdateQuestRequest;
import com.mos.quest.service.QuestService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/quests")
@RequiredArgsConstructor
public class AdminQuestController {

    private final QuestService questService;

    @PostMapping
    public QuestResponse createQuest(@Valid @RequestBody CreateQuestRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return questService.createQuest(admin.gameSessionId(), request, admin.userId());
    }

    @PatchMapping("/{id}")
    public QuestResponse updateQuest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestRequest request
    ) {
        var admin = SecurityUtils.getCurrentUser();
        return questService.updateQuest(id, admin.gameSessionId(), request);
    }
}
