package com.mos.location.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.LocationNotFoundException;
import com.mos.common.exception.LocationSessionMismatchException;
import com.mos.location.dto.AdminLocationPointResponse;
import com.mos.location.dto.CreateLocationRequest;
import com.mos.location.dto.LocationPointResponse;
import com.mos.location.dto.UpdateLocationRequest;
import com.mos.location.entity.LocationPoint;
import com.mos.location.entity.PlayerLocationDiscovery;
import com.mos.location.repository.LocationPointRepository;
import com.mos.location.repository.PlayerLocationDiscoveryRepository;
import com.mos.victory.service.VictoryConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationPointRepository locationPointRepository;
    private final PlayerLocationDiscoveryRepository playerLocationDiscoveryRepository;
    private final AuditService auditService;
    private final VictoryConditionService victoryConditionService;

    @Transactional
    public AdminLocationPointResponse createLocation(UUID gameSessionId, CreateLocationRequest request) {
        LocationPoint location = locationPointRepository.save(LocationPoint.builder()
                .gameSessionId(gameSessionId)
                .name(request.name())
                .description(request.description())
                .x(request.x())
                .y(request.y())
                .zone(request.zone())
                .hidden(request.hidden())
                .build());

        return AdminLocationPointResponse.from(location);
    }

    @Transactional
    public AdminLocationPointResponse updateLocation(
            UUID locationId,
            UUID gameSessionId,
            UpdateLocationRequest request
    ) {
        LocationPoint location = getLocationForSession(locationId, gameSessionId);

        if (request.name() != null) {
            location.setName(request.name());
        }
        if (request.description() != null) {
            location.setDescription(request.description());
        }
        if (request.x() != null) {
            location.setX(request.x());
        }
        if (request.y() != null) {
            location.setY(request.y());
        }
        if (request.zone() != null) {
            location.setZone(request.zone());
        }
        if (request.hidden() != null) {
            location.setHidden(request.hidden());
        }

        return AdminLocationPointResponse.from(locationPointRepository.save(location));
    }

    @Transactional(readOnly = true)
    public List<LocationPointResponse> getLocations(UUID userId, UUID gameSessionId) {
        List<LocationPoint> locations = locationPointRepository.findByGameSessionIdOrderByZoneAscNameAsc(gameSessionId);
        Set<UUID> discoveredIds = getDiscoveredLocationIds(userId, gameSessionId);

        return locations.stream()
                .map(location -> LocationPointResponse.forPlayer(location, isDiscovered(location, discoveredIds)))
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationPointResponse getLocation(UUID locationId, UUID userId, UUID gameSessionId) {
        LocationPoint location = getLocationForSession(locationId, gameSessionId);
        boolean discovered = isDiscovered(location, getDiscoveredLocationIds(userId, gameSessionId));
        return LocationPointResponse.forPlayer(location, discovered);
    }

    @Transactional
    public LocationPointResponse discoverLocation(UUID userId, UUID locationId, UUID gameSessionId) {
        LocationPoint location = getLocationForSession(locationId, gameSessionId);

        var existing = playerLocationDiscoveryRepository.findByUserIdAndLocationPointId(userId, locationId);
        if (existing.isPresent()) {
            return LocationPointResponse.forPlayer(location, true);
        }

        playerLocationDiscoveryRepository.save(PlayerLocationDiscovery.builder()
                .userId(userId)
                .locationPoint(location)
                .gameSessionId(gameSessionId)
                .build());

        auditService.log(
                userId,
                gameSessionId,
                AuditAction.LOCATION_DISCOVER,
                "LocationPoint",
                location.getId().toString(),
                "Location discovered: " + location.getName(),
                Map.of(
                        "locationId", location.getId().toString(),
                        "zone", location.getZone(),
                        "hidden", location.getHidden()
                )
        );

        victoryConditionService.checkAfterGameDataChange(userId, gameSessionId);

        return LocationPointResponse.forPlayer(location, true);
    }

    private Set<UUID> getDiscoveredLocationIds(UUID userId, UUID gameSessionId) {
        return playerLocationDiscoveryRepository.findByUserIdAndGameSessionId(userId, gameSessionId).stream()
                .map(discovery -> discovery.getLocationPoint().getId())
                .collect(Collectors.toSet());
    }

    private boolean isDiscovered(LocationPoint location, Set<UUID> discoveredIds) {
        return discoveredIds.contains(location.getId());
    }

    private LocationPoint getLocationForSession(UUID locationId, UUID gameSessionId) {
        LocationPoint location = locationPointRepository.findById(locationId)
                .orElseThrow(LocationNotFoundException::new);

        if (!location.getGameSessionId().equals(gameSessionId)) {
            throw new LocationSessionMismatchException();
        }

        return location;
    }
}
