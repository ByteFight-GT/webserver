package com.example.botfightwebserver.matchMaking.application;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.infra.GameMatchRepository;
import com.example.botfightwebserver.matchMaking.domain.MATCHMAKING_REASON;
import com.example.botfightwebserver.matchMaking.domain.MatchmakingEvent;
import com.example.botfightwebserver.matchMaking.infra.MatchMakingEventRepository;
import com.example.botfightwebserver.matchMaking.infra.MatchMakingProperties;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final MatchMakingProperties props;
    private final PermissionsService permissionsService;

    @Transactional
    public MatchmakingEvent createEvent(MATCHMAKING_REASON reason) {
        List<Team> playableTeams = teamService.getTeamsWithSubmission();
        List<GameMatch> gameMatches = matchMaker.generateMatches(playableTeams);

        MatchmakingEvent event = MatchmakingEvent.builder()
            .build();

        matchMakingEventRepository.save(event);

        for(GameMatch match : gameMatches) {
            match.setMatchmakingEvent(event);
            gameMatchRepository.save(match);
        }

        return event;
    }

    public MatchmakingEvent queueEvent(MatchmakingEvent event) {
//        List<GameMatch> gameMatches = gameMatchRepository.findByMatchmakingEvent(event);
//
//        for(GameMatch gameMatch : gameMatches) {
//            gameMatchService.queueMatch(gameMatch);
//        }

        return event;
    }

    public Optional<MatchmakingEvent> getLastScheduledEvent() {
        return matchMakingEventRepository.findFirstByOrderByCreatedAtDesc();
    }

    public boolean isEnabled() {
        return props.isEnabled() && permissionsService.get().getRunScheduledMatchmaking();
    }
}
