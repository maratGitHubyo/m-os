package com.mos.item.controller;

import com.mos.item.dto.PlayerItemResponse;
import com.mos.item.service.ItemService;
import com.mos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ItemService itemService;

    @GetMapping
    public List<PlayerItemResponse> getMyInventory() {
        var currentUser = SecurityUtils.getCurrentUser();
        return itemService.getInventory(currentUser.userId(), currentUser.gameSessionId());
    }

    @GetMapping("/{id}")
    public PlayerItemResponse getInventoryItem(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return itemService.getInventoryItem(id, currentUser.userId(), currentUser.gameSessionId());
    }
}
