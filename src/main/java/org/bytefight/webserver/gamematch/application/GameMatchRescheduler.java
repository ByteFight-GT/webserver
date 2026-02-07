package org.bytefight.webserver.gamematch.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class  GameMatchRescheduler {
    private final GameMatchService gameMatchService;

    @Scheduled(cron = "0 */30 * * * *")
    public void reschedule() {
        gameMatchService.rescheduleStaleMatches(false);
    }
}