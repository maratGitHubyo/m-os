package com.mos.numbers.controller;

import com.mos.numbers.dto.NumberCollectionProgressResponse;
import com.mos.numbers.dto.PlayerNumberResponse;
import com.mos.numbers.service.NumberService;
import com.mos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/numbers")
@RequiredArgsConstructor
public class NumberController {

    private final NumberService numberService;

    @GetMapping("/me")
    public List<PlayerNumberResponse> getMyNumbers() {
        var currentUser = SecurityUtils.getCurrentUser();
        return numberService.getMyNumbers(currentUser.userId(), currentUser.gameSessionId());
    }

    @GetMapping("/progress")
    public NumberCollectionProgressResponse getProgress() {
        var currentUser = SecurityUtils.getCurrentUser();
        return numberService.getProgress(currentUser.userId(), currentUser.gameSessionId());
    }
}
