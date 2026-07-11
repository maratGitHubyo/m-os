package com.mos.common.audit.controller;

import com.mos.common.audit.dto.AuditLogResponse;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.security.MosUserPrincipal;
import com.mos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping
    public Page<AuditLogResponse> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) AuditAction action,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        MosUserPrincipal currentUser = SecurityUtils.getCurrentUser();

        return auditService.findLogs(
                currentUser.gameSessionId(),
                userId,
                action,
                pageable
        );
    }
}
