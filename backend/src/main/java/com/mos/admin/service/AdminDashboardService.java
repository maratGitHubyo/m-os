package com.mos.admin.service;

import com.mos.admin.dto.AdminDashboardResponse;
import com.mos.common.exception.BusinessException;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.location.repository.LocationPointRepository;
import com.mos.qrcode.repository.QrCodeRepository;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.repository.QuestRepository;
import com.mos.session.dto.GameSessionResponse;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.wallet.dto.LeaderboardEntryResponse;
import com.mos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final GameSessionRepository gameSessionRepository;
    private final GameConfigRepository gameConfigRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final QuestRepository questRepository;
    private final LocationPointRepository locationPointRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final QrCodeRepository qrCodeRepository;
    private final WalletService walletService;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(UUID gameSessionId) {
        GameSession session = gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));

        if (session.getConfig() == null) {
            gameConfigRepository.findByGameSessionId(gameSessionId).ifPresent(session::setConfig);
        }

        List<LeaderboardEntryResponse> leaderboard = walletService.getCoinLeaderboard(gameSessionId);

        return new AdminDashboardResponse(
                GameSessionResponse.from(session),
                sessionParticipantRepository.countByGameSessionIdAndRole(gameSessionId, ParticipantRole.PLAYER),
                questRepository.countByGameSessionIdAndStatus(gameSessionId, QuestDefinitionStatus.ACTIVE),
                locationPointRepository.countByGameSessionId(gameSessionId),
                itemTemplateRepository.countByGameSessionId(gameSessionId),
                qrCodeRepository.countByGameSessionId(gameSessionId),
                leaderboard
        );
    }
}
