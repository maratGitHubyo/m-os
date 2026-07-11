package com.mos.qrcode.repository;

import com.mos.qrcode.entity.QrScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QrScanRepository extends JpaRepository<QrScan, UUID> {

    boolean existsByUserIdAndQrCodeId(UUID userId, UUID qrCodeId);

    long countByQrCodeId(UUID qrCodeId);
}
