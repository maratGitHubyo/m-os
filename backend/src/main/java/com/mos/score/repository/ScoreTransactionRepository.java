package com.mos.score.repository;

import com.mos.score.entity.ScoreTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScoreTransactionRepository extends JpaRepository<ScoreTransaction, UUID> {
}
