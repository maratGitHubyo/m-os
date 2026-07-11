package com.mos.score.controller;

import com.mos.score.dto.LeaderboardEntryResponse;
import com.mos.score.dto.PlayerScoreResponse;
import com.mos.score.service.ScoreService;
import com.mos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Score")
public class ScoreController {

    private final ScoreService scoreService;

    @GetMapping("/api/leaderboard")
    public List<LeaderboardEntryResponse> getLeaderboard() {
        var currentUser = SecurityUtils.getCurrentUser();
        return scoreService.getLeaderboard(currentUser.gameSessionId());
    }

    @GetMapping("/api/score/me")
    public List<PlayerScoreResponse> getMyScores() {
        var currentUser = SecurityUtils.getCurrentUser();
        return scoreService.getMyScores(currentUser.userId(), currentUser.gameSessionId());
    }
}
