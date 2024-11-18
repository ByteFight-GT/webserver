package com.example.botfightwebserver.gameMatch;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

// still need to schedule
@RequiredArgsConstructor
public class GameMatchRescheduler {

    private GameMatchService gameMatchService;

    public List<GameMatchJob> reschedule() {
        return gameMatchService.rescheduleFailedAndStaleMatches();
    }
}
