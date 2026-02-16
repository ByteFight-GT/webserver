package org.bytefight.webserver.glicko.application;

import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamStatsService {
    private final TeamStatsRepository teamStatsRepository;
    private final LadderRepository ladderRepository;

    public TeamStats getAggregateWDL(Team team){
        List<Ladder> ladders = ladderRepository.findAllByCompetition(team.getCompetition());

        int matchesPlayed = 0;
        int wins = 0;
        int draws = 0;
        int losses = 0;

        for(Ladder ladder: ladders){
            Optional<TeamStats> teamStats = teamStatsRepository.findByTeamAndLadder(team, ladder.getLadder());

            if (teamStats.isPresent()) {
                wins += teamStats.get().getWins();
                losses += teamStats.get().getLosses();
                draws += teamStats.get().getDraws();
                matchesPlayed += teamStats.get().getMatchesPlayed();
            }
        }

        return TeamStats.builder()
                .team(team)
                .competition(team.getCompetition())
                .ladder("")
                .wins(wins)
                .losses(losses)
                .draws(draws)
                .glickoRating(0)
                .glickoRd(0)
                .glickoVolatility(0)
                .build();

    }

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
