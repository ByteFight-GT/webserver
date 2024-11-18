package com.example.botfightwebserver.gameMatch;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@RequiredArgsConstructor
public class GameMatchRescheduler {

    private GameMatchService gameMatchService;

    // may want to change to return GameMatchJobs
    public List<GameMatchJob> reschedule() {
        return gameMatchService.rescheduleFailedAndStaleMatches();
    }
}
