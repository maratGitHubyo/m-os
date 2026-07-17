package com.mos.quest.scheduler;

import com.mos.quest.service.QuestBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestAutoDistributeScheduler {

    private final QuestBroadcastService questBroadcastService;

    @Scheduled(fixedDelayString = "60000")
    public void tick() {
        try {
            questBroadcastService.runDueAutoDistributions();
        } catch (Exception ex) {
            log.error("Quest auto-distribute tick failed", ex);
        }
    }
}
