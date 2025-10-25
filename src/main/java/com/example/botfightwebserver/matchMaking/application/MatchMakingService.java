package com.example.botfightwebserver.matchMaking.application;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.GameMatchJob;
import com.example.botfightwebserver.gameMatch.infra.GameMatchRepository;
import com.example.botfightwebserver.matchMaking.domain.MATCHMAKING_REASON;
import com.example.botfightwebserver.matchMaking.infra.MatchMakingEventRepository;
import com.example.botfightwebserver.matchMaking.domain.MatchMakingEvent;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchMakingService {
    private final MatchMaker matchMaker;
    private final TeamService teamService;
    private final GameMatchService gameMatchService;
    private final GameMatchRepository gameMatchRepository;
    private final MatchMakingEventRepository matchMakingEventRepository;
    private final ClockConfig clockConfig;

    public MatchMakingEvent createEvent(MATCHMAKING_REASON reason) {
        List<Team> playableTeams = teamService.getTeamsWithSubmission();
        List<GameMatch> gameMatches = matchMaker.generateMatches(playableTeams);

        MatchMakingEvent event = MatchMakingEvent.builder()
            .numberTeams(playableTeams.size())
            .numberMatches(gameMatches.size())
            .creationDateTime(LocalDateTime.now(clockConfig.clock()))
            .reason(reason)
            .build();

        matchMakingEventRepository.save(event);

        for(GameMatch match : gameMatches) {
            match.setMatchmakingEvent(event);
            gameMatchRepository.save(match);
        }

        return event;
    }

    public MatchMakingEvent queueEvent(MatchMakingEvent event) {
        List<GameMatch> gameMatches = gameMatchRepository.findByMatchmakingEvent(event);

        for(GameMatch gameMatch : gameMatches) {
            gameMatchService.queueMatch(gameMatch);
        }

        return event;
    }

    public Optional<MatchMakingEvent> getLastScheduledEvent() {
        return matchMakingEventRepository.findFirstByReasonOrderByCreationDateTimeDesc(MATCHMAKING_REASON.SCHEDULED);
    }
}
