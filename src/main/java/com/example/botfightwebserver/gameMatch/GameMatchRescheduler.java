package com.example.botfightwebserver.gameMatch;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

// still need to schedule
@RequiredArgsConstructor
public class  GameMatchRescheduler {

    private final GameMatchService gameMatchService;

    @Scheduled(cron = "0 30 */2 * * *")
    public List<GameMatchJob> reschedule() {
        return gameMatchService.rescheduleFailedAndStaleMatches();
    }
}
