package com.mos.qrcode.repository;

import com.mos.qrcode.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {

    List<QrCode> findByGameSessionIdOrderByCreatedAtAsc(UUID gameSessionId);

    Optional<QrCode> findByCodeAndGameSessionId(String code, UUID gameSessionId);

    Optional<QrCode> findByPublicId(UUID publicId);

    boolean existsByCodeAndGameSessionId(String code, UUID gameSessionId);

    long countByGameSessionId(UUID gameSessionId);
}
