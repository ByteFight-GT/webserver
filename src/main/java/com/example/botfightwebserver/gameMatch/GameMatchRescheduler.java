package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class GameMatchRescheduler {

    private GameMatchService gameMatchService;
    private RabbitMQService rabbitMQService;

    public List<GameMatch> rescheduleStaleAndFailedMatches() {
        List<GameMatch> matchesToReschedule = Stream.concat(gameMatchService.getFailedMatches().stream(),
            gameMatchService.getStaleWaitingMatches().stream()).toList();

        matchesToReschedule.stream().forEach(match -> {
            System.out.println();
        });
        return matchesToReschedule;
    }
}
