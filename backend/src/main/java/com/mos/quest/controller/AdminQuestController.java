package com.mos.quest.controller;

import com.mos.quest.dto.BroadcastQuestsRequest;
import com.mos.quest.dto.BroadcastQuestsResponse;
import com.mos.quest.dto.CreateQuestRequest;
import com.mos.quest.dto.QuestAutoDistributeStatusResponse;
import com.mos.quest.dto.QuestResponse;
import com.mos.quest.dto.UpdateQuestRequest;
import com.mos.quest.service.QuestBroadcastService;
import com.mos.quest.service.QuestService;
import com.mos.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/quests")
@RequiredArgsConstructor
@Tag(name = "Admin Quests")
public class AdminQuestController {

    private final QuestService questService;
    private final QuestBroadcastService questBroadcastService;

    @GetMapping
    public java.util.List<QuestResponse> listQuests() {
        var admin = SecurityUtils.getCurrentUser();
        return questService.listQuestsForAdmin(admin.gameSessionId());
    }

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

    @PostMapping("/close-incomplete")
    public Map<String, Object> closeIncompleteQuests() {
        var admin = SecurityUtils.getCurrentUser();
        int closedPlayerQuests = questService.closeIncompleteQuests(admin.gameSessionId(), admin.userId());
        return Map.of(
                "success", true,
                "closedPlayerQuests", closedPlayerQuests,
                "message", "Incomplete quests closed"
        );
    }

    @PostMapping("/broadcast")
    public BroadcastQuestsResponse broadcast(@Valid @RequestBody BroadcastQuestsRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return questBroadcastService.broadcast(admin.gameSessionId(), request.count(), admin.userId());
    }

    @GetMapping("/auto-distribute")
    public QuestAutoDistributeStatusResponse autoDistributeStatus() {
        var admin = SecurityUtils.getCurrentUser();
        return questBroadcastService.status(admin.gameSessionId());
    }

    @PostMapping("/auto-distribute/start")
    public QuestAutoDistributeStatusResponse startAutoDistribute() {
        var admin = SecurityUtils.getCurrentUser();
        return questBroadcastService.startAuto(admin.gameSessionId(), admin.userId());
    }

    @PostMapping("/auto-distribute/stop")
    public QuestAutoDistributeStatusResponse stopAutoDistribute() {
        var admin = SecurityUtils.getCurrentUser();
        return questBroadcastService.stopAuto(admin.gameSessionId(), admin.userId());
    }
}
