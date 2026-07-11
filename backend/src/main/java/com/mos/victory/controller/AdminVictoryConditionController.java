package com.mos.victory.controller;

import com.mos.security.SecurityUtils;
import com.mos.victory.dto.CreateVictoryConditionRequest;
import com.mos.victory.dto.VictoryConditionAdminStatusResponse;
import com.mos.victory.dto.VictoryConditionResponse;
import com.mos.victory.service.VictoryConditionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/victory-conditions")
@RequiredArgsConstructor
@Tag(name = "Admin Victory")
public class AdminVictoryConditionController {

    private final VictoryConditionService victoryConditionService;

    @PostMapping
    public VictoryConditionResponse createCondition(@Valid @RequestBody CreateVictoryConditionRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return victoryConditionService.createCondition(admin.gameSessionId(), request);
    }

    @GetMapping("/status")
    public VictoryConditionAdminStatusResponse getStatus() {
        var admin = SecurityUtils.getCurrentUser();
        return victoryConditionService.getAdminStatus(admin.gameSessionId());
    }
}
