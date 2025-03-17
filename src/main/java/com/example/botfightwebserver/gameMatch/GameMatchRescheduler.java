package com.example.botfightwebserver.gameMatch;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "is-prod-env", havingValue = "true")
public class  GameMatchRescheduler {

    private final GameMatchService gameMatchService;

    @Scheduled(cron = "0 30 */2 * * *")
    public List<GameMatchJob> reschedule() {
        return gameMatchService.rescheduleStaleMatches();
    }
}