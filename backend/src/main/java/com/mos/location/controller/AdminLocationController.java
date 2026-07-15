package com.mos.location.controller;

import com.mos.location.dto.AdminLocationPointResponse;
import com.mos.location.dto.CreateLocationRequest;
import com.mos.location.dto.UpdateLocationRequest;
import com.mos.location.service.LocationService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/locations")
@RequiredArgsConstructor
@Tag(name = "Admin Locations")
public class AdminLocationController {

    private final LocationService locationService;

    @PostMapping
    public AdminLocationPointResponse createLocation(@Valid @RequestBody CreateLocationRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return locationService.createLocation(admin.gameSessionId(), request);
    }

    @PatchMapping("/{id}")
    public AdminLocationPointResponse updateLocation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLocationRequest request
    ) {
        var admin = SecurityUtils.getCurrentUser();
        return locationService.updateLocation(id, admin.gameSessionId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable UUID id) {
        var admin = SecurityUtils.getCurrentUser();
        locationService.deleteLocation(id, admin.gameSessionId(), admin.userId());
    }
}
