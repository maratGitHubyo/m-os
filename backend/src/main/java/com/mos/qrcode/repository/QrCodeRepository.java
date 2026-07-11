package com.mos.qrcode.repository;

import com.mos.qrcode.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {

    Optional<QrCode> findByCodeAndGameSessionId(String code, UUID gameSessionId);

    boolean existsByCodeAndGameSessionId(String code, UUID gameSessionId);

    long countByGameSessionId(UUID gameSessionId);
}
