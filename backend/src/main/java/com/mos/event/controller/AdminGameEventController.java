package com.mos.event.controller;

import com.mos.event.dto.CreateGameEventRequest;
import com.mos.event.dto.GameEventResponse;
import com.mos.event.dto.UpdateGameEventStatusRequest;
import com.mos.event.service.GameEventService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@Tag(name = "Admin Events")
public class AdminGameEventController {

    private final GameEventService gameEventService;

    @PostMapping
    public GameEventResponse createEvent(@Valid @RequestBody CreateGameEventRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return gameEventService.createEvent(admin.gameSessionId(), request, admin.userId());
    }

    @PatchMapping("/{id}/status")
    public GameEventResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGameEventStatusRequest request
    ) {
        var admin = SecurityUtils.getCurrentUser();
        return gameEventService.updateStatus(id, admin.gameSessionId(), request.status(), admin.userId());
    }
}
