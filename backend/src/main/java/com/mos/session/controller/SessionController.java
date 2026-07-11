package com.mos.session.controller;

import com.mos.session.dto.GameSessionResponse;
import com.mos.session.service.GameSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final GameSessionService gameSessionService;

    @GetMapping("/current")
    public GameSessionResponse getCurrentSession() {
        return gameSessionService.getCurrentSession();
    }
}
