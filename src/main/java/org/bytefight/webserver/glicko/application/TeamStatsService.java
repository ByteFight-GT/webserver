package org.bytefight.webserver.glicko.application;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.domain.dto.TeamStatsDto;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamStatsService {
  private final TeamStatsRepository teamStatsRepository;
  private final LadderRepository ladderRepository;

  public TeamStatsDto getAggregateWDL(Team team) {
    List<String> ladders =
        ladderRepository.findAllByCompetition(team.getCompetition()).stream()
            .map(Ladder::getLadder)
            .collect(Collectors.toList());
    return TeamStatsDto.from(
        teamStatsRepository.getTeamStatsAggregateByTeamAndLadder(team, ladders));
  }

  public TeamStats getTeamStatsCreateIfNotExist(Team team, String ladderSlug) {
    Optional<TeamStats> teamStats = teamStatsRepository.findByTeamAndLadder(team, ladderSlug);

    if (teamStats.isPresent()) {
      return teamStats.get();
    }

    Ladder ladder =
        ladderRepository
            .findByCompetitionAndLadder(team.getCompetition(), ladderSlug)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Ladder "
                            + ladderSlug
                            + " not found in competition "
                            + team.getCompetition().getSlug()));

    TeamStats baseTeamStats =
        TeamStats.builder()
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
