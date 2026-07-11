package com.mos.item.controller;

import com.mos.item.dto.AdminGrantItemRequest;
import com.mos.item.dto.CreateItemTemplateRequest;
import com.mos.item.dto.ItemTemplateResponse;
import com.mos.item.dto.PlayerItemResponse;
import com.mos.item.service.ItemService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/items")
@RequiredArgsConstructor
@Tag(name = "Admin Items")
public class AdminItemController {

    private final ItemService itemService;

    @PostMapping("/templates")
    public ItemTemplateResponse createTemplate(@Valid @RequestBody CreateItemTemplateRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return itemService.createTemplate(admin.gameSessionId(), request);
    }

    @GetMapping("/templates")
    public List<ItemTemplateResponse> listTemplates() {
        var admin = SecurityUtils.getCurrentUser();
        return itemService.getTemplates(admin.gameSessionId());
    }

    @PostMapping("/grant")
    public PlayerItemResponse grantItem(@Valid @RequestBody AdminGrantItemRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return itemService.grantItem(
                request.userId(),
                request.itemTemplateId(),
                admin.gameSessionId(),
                admin.userId()
        );
    }
}
