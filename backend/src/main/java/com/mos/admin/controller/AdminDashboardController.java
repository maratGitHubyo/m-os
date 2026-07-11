package com.mos.admin.controller;

import com.mos.admin.dto.AdminDashboardResponse;
import com.mos.admin.service.AdminDashboardService;
import com.mos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public AdminDashboardResponse getDashboard() {
        var admin = SecurityUtils.getCurrentUser();
        return adminDashboardService.getDashboard(admin.gameSessionId());
    }
}
