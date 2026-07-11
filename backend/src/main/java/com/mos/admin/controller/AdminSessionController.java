package com.mos.admin.controller;

import com.mos.admin.service.AdminSessionService;
import com.mos.security.SecurityUtils;
import com.mos.session.dto.GameSessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/session")
@RequiredArgsConstructor
public class AdminSessionController {

    private final AdminSessionService adminSessionService;

    @PostMapping("/start")
    public GameSessionResponse startSession() {
        var admin = SecurityUtils.getCurrentUser();
        return adminSessionService.startSession(admin.gameSessionId(), admin.userId());
    }

    @PostMapping("/pause")
    public GameSessionResponse pauseSession() {
        var admin = SecurityUtils.getCurrentUser();
        return adminSessionService.pauseSession(admin.gameSessionId(), admin.userId());
    }

    @PostMapping("/finish")
    public GameSessionResponse finishSession() {
        var admin = SecurityUtils.getCurrentUser();
        return adminSessionService.finishSession(admin.gameSessionId(), admin.userId());
    }
}
