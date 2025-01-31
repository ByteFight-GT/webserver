package com.example.botfightwebserver.gameMatch;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class  GameMatchRescheduler {

    private final GameMatchService gameMatchService;

    @Scheduled(cron = "0 30 */2 * * *")
    public List<GameMatchJob> reschedule() {
        return gameMatchService.rescheduleFailedAndStaleMatches();
    }
}
