package com.mos.score.controller;

import com.mos.score.dto.AdminScoreChangeRequest;
import com.mos.score.dto.ScoreTransactionResponse;
import com.mos.score.service.ScoreService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/score")
@RequiredArgsConstructor
public class AdminScoreController {

    private final ScoreService scoreService;

    @PostMapping("/{userId}/add")
    public ScoreTransactionResponse addPoints(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminScoreChangeRequest request
    ) {
        var admin = SecurityUtils.getCurrentUser();
        return scoreService.addPoints(
                userId,
                admin.gameSessionId(),
                request.category(),
                request.points(),
                request.reason(),
                admin.userId()
        );
    }

    @PostMapping("/{userId}/subtract")
    public ScoreTransactionResponse subtractPoints(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminScoreChangeRequest request
    ) {
        var admin = SecurityUtils.getCurrentUser();
        return scoreService.subtractPoints(
                userId,
                admin.gameSessionId(),
                request.category(),
                request.points(),
                request.reason(),
                admin.userId()
        );
    }
}
