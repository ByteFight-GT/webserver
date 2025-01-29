package com.example.botfightwebserver.matchMaking;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduledMatchMaker {

    private final MatchMaker matchMaker;

    @Scheduled(cron = "0 */10 * * * *")
    public void scheduleMatchGeneration() {
        System.out.println("Scheduled Match Generation");
        matchMaker.generateMatches();
    }
}

