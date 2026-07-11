package com.mos.wallet.transfer.service;

import com.mos.wallet.transfer.dto.CoinTransferResponse;
import com.mos.wallet.transfer.dto.CreateCoinTransferRequest;
import com.mos.wallet.transfer.entity.CoinTransfer;
import com.mos.wallet.transfer.repository.CoinTransferRepository;
import com.mos.wallet.service.WalletService;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.SessionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoinTransferService {

    private final WalletService walletService;
    private final CoinTransferRepository coinTransferRepository;
    private final SessionParticipantRepository sessionParticipantRepository;

    @Transactional
    public CoinTransferResponse transfer(
            UUID senderUserId,
            CreateCoinTransferRequest request,
            UUID gameSessionId
    ) {
        CoinTransfer transfer = walletService.transferCoins(
                senderUserId,
                request.receiverUserId(),
                request.amount(),
                gameSessionId
        );

        Map<UUID, String> nicknames = nicknamesByUserId(gameSessionId);
        return CoinTransferResponse.from(
                transfer,
                nicknames.get(transfer.getSenderUserId()),
                nicknames.get(transfer.getReceiverUserId())
        );
    }

    @Transactional(readOnly = true)
    public Page<CoinTransferResponse> getTransfers(UUID userId, UUID gameSessionId, Pageable pageable) {
        Map<UUID, String> nicknames = nicknamesByUserId(gameSessionId);

        return coinTransferRepository.findByGameSessionAndParticipant(gameSessionId, userId, pageable)
                .map(transfer -> CoinTransferResponse.from(
                        transfer,
                        nicknames.get(transfer.getSenderUserId()),
                        nicknames.get(transfer.getReceiverUserId())
                ));
    }

    private Map<UUID, String> nicknamesByUserId(UUID gameSessionId) {
        return sessionParticipantRepository.findByGameSessionId(gameSessionId).stream()
                .collect(Collectors.toMap(
                        participant -> participant.getUser().getId(),
                        SessionParticipant::getNicknameSnapshot,
                        (left, right) -> left
                ));
    }
}
