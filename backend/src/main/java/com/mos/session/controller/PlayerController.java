package com.mos.session.controller;

import com.mos.security.SecurityUtils;
import com.mos.session.dto.SessionPlayerResponse;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.repository.SessionParticipantRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Tag(name = "Players")
public class PlayerController {

    private final SessionParticipantRepository sessionParticipantRepository;

    @GetMapping
    public List<SessionPlayerResponse> listSessionPlayers() {
        var currentUser = SecurityUtils.getCurrentUser();
        return sessionParticipantRepository
                .findByGameSessionIdAndRole(currentUser.gameSessionId(), ParticipantRole.PLAYER)
                .stream()
                .map(participant -> new SessionPlayerResponse(
                        participant.getUser().getId(),
                        participant.getNicknameSnapshot()
                ))
                .sorted(Comparator.comparing(SessionPlayerResponse::nickname))
                .toList();
    }
}
