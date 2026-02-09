package org.bytefight.webserver.glicko.application;

import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.glicko.domain.Ladder;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.infra.LadderRepository;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamStatsService {
    private final TeamStatsRepository teamStatsRepository;
    private final LadderRepository ladderRepository;

    public TeamStats getTeamStatsCreateIfNotExist(Team team, String ladderSlug) {
        Optional<TeamStats> teamStats = teamStatsRepository.findByTeamAndLadder(team, ladderSlug);

        if (teamStats.isPresent()) {
            return teamStats.get();
        }

        Ladder ladder = ladderRepository.findByCompetitionAndLadder(team.getCompetition(), ladderSlug)
                .orElseThrow(() -> new RuntimeException("Ladder " + ladderSlug + " not found in competition " + team.getCompetition().getSlug()));

        TeamStats baseTeamStats = TeamStats.builder()
                .team(team)
                .competition(team.getCompetition())
                .ladder(ladderSlug)
                .glickoRating(ladder.getGlickoDefaultRating())
                .glickoRd(ladder.getGlickoDefaultRd())
                .glickoVolatility(ladder.getGlickoSigmaDefault())
                .build();

        return teamStatsRepository.save(baseTeamStats);
    }
}
