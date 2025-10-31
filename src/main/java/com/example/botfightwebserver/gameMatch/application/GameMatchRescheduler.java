package com.example.botfightwebserver.gameMatch.application;

import com.example.botfightwebserver.gameMatch.domain.GameMatchJob;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class  GameMatchRescheduler {
    private final GameMatchService gameMatchService;

    @Scheduled(cron = "0 */30 * * * *")
    public void reschedule() {
        gameMatchService.rescheduleStaleMatches(false);
    }
}