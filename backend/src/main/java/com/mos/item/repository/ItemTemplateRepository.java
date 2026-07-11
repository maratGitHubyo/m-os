package com.mos.item.repository;

import com.mos.item.entity.ItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemTemplateRepository extends JpaRepository<ItemTemplate, UUID> {

    List<ItemTemplate> findByGameSessionIdOrderByNameAsc(UUID gameSessionId);

    long countByGameSessionId(UUID gameSessionId);
}
