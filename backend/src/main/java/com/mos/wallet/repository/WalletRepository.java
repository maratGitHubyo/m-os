package com.mos.wallet.repository;

import com.mos.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserIdAndGameSessionId(UUID userId, UUID gameSessionId);

    List<Wallet> findByGameSessionId(UUID gameSessionId);
}
