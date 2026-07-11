package com.mos.event.controller;

import com.mos.event.dto.GameEventResponse;
import com.mos.event.service.GameEventService;
import com.mos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class GameEventController {

    private final GameEventService gameEventService;

    @GetMapping
    public List<GameEventResponse> getEvents() {
        var currentUser = SecurityUtils.getCurrentUser();
        return gameEventService.getEvents(currentUser.gameSessionId());
    }

    @GetMapping("/{id}")
    public GameEventResponse getEvent(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return gameEventService.getEvent(id, currentUser.gameSessionId());
    }
}
