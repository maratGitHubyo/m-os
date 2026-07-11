package com.mos.victory.controller;

import com.mos.security.SecurityUtils;
import com.mos.victory.dto.VictoryConditionResponse;
import com.mos.victory.service.VictoryConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/victory-conditions")
@RequiredArgsConstructor
@Tag(name = "Victory")
public class VictoryConditionController {

    private final VictoryConditionService victoryConditionService;

    @GetMapping
    public List<VictoryConditionResponse> getVictoryConditions() {
        var currentUser = SecurityUtils.getCurrentUser();
        return victoryConditionService.getConditionsForPlayer(
                currentUser.userId(),
                currentUser.gameSessionId()
        );
    }
}
