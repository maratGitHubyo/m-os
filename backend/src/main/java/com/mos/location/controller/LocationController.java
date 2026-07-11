package com.mos.location.controller;

import com.mos.location.dto.LocationPointResponse;
import com.mos.location.service.LocationService;
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
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Locations")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationPointResponse> getLocations() {
        var currentUser = SecurityUtils.getCurrentUser();
        return locationService.getLocations(currentUser.userId(), currentUser.gameSessionId());
    }

    @GetMapping("/{id}")
    public LocationPointResponse getLocation(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return locationService.getLocation(id, currentUser.userId(), currentUser.gameSessionId());
    }
}
