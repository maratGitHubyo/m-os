package com.mos.quest.repository;

import com.mos.quest.entity.Quest;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestRepository extends JpaRepository<Quest, UUID> {

    Optional<Quest> findByIdAndGameSessionId(UUID id, UUID gameSessionId);

    List<Quest> findByGameSessionIdAndStatusOrderByCreatedAtAsc(UUID gameSessionId, QuestDefinitionStatus status);

    long countByGameSessionIdAndStatus(UUID gameSessionId, QuestDefinitionStatus status);

    List<Quest> findByGameSessionIdAndStatusAndTypeOrderByCreatedAtAsc(
            UUID gameSessionId,
            QuestDefinitionStatus status,
            QuestType type
    );
}
