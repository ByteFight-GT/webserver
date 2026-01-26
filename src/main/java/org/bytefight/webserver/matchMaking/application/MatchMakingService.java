package org.bytefight.webserver.matchMaking.application;

import org.bytefight.webserver.config.ClockConfig;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.matchMaking.domain.MATCHMAKING_REASON;
import org.bytefight.webserver.matchMaking.domain.MatchmakingEvent;
import org.bytefight.webserver.matchMaking.infra.MatchMakingEventRepository;
import org.bytefight.webserver.matchMaking.infra.MatchMakingProperties;
import org.bytefight.webserver.permissions.application.PermissionsService;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
