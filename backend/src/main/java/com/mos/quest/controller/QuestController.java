package com.mos.quest.controller;

import com.mos.quest.dto.CompleteQuestRequest;
import com.mos.quest.dto.PlayerQuestResponse;
import com.mos.quest.dto.QuestResponse;
import com.mos.quest.dto.StartQuestResponse;
import com.mos.quest.service.QuestService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
@Tag(name = "Quests")
public class QuestController {

    private final QuestService questService;

    @GetMapping
    public List<QuestResponse> getAvailableQuests() {
        var currentUser = SecurityUtils.getCurrentUser();
        return questService.getAvailableQuests(currentUser.userId(), currentUser.gameSessionId());
    }

    @GetMapping("/me")
    public List<PlayerQuestResponse> getMyQuests() {
        var currentUser = SecurityUtils.getCurrentUser();
        return questService.getMyQuests(currentUser.userId(), currentUser.gameSessionId());
    }

    @PostMapping("/{id}/start")
    public StartQuestResponse startQuest(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return questService.startQuest(id, currentUser.userId(), currentUser.gameSessionId());
    }

    @PostMapping("/{id}/complete")
    public PlayerQuestResponse completeQuest(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CompleteQuestRequest request
    ) {
        var currentUser = SecurityUtils.getCurrentUser();
        return questService.completeQuest(
                id,
                currentUser.userId(),
                currentUser.gameSessionId(),
                request != null ? request : new CompleteQuestRequest(null)
        );
    }
}
