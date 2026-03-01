package org.bytefight.webserver.ladder.application;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.application.TeamStatsService;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LadderService {
  private final TeamStatsService teamStatsService;
  private final LadderRepository ladderRepository;
  private final TeamRepository teamRepository;

  public Ladder createLadder(
      Competition competition,
      String ladderSlug,
      double glickoDefaultRating,
      double glickoDefaultRd,
      double glickoRdMax,
      double glickoRdMin,
      double glickoPhiInflationPerDay,
      double glickoTau,
      double glickoSigmaDefault,
      double glickoSigmaMin,
      double glickoSigmaMax) {

    ladderRepository
        .findByCompetitionAndLadder(competition, ladderSlug)
        .ifPresent(
            existing -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "Ladder already exists");
            });

    Ladder ladder = new Ladder();
    ladder.setCompetition(competition);
    ladder.setLadder(ladderSlug);
    ladder.setGlickoDefaultRating(glickoDefaultRating);
    ladder.setGlickoDefaultRd(glickoDefaultRd);
    ladder.setGlickoRdMax(glickoRdMax);
    ladder.setGlickoRdMin(glickoRdMin);
    ladder.setGlickoPhiInflationPerDay(glickoPhiInflationPerDay);
    ladder.setGlickoTau(glickoTau);
    ladder.setGlickoSigmaDefault(glickoSigmaDefault);
    ladder.setGlickoSigmaMin(glickoSigmaMin);
    ladder.setGlickoSigmaMax(glickoSigmaMax);

    ladder = ladderRepository.save(ladder);

    for (Team team : teamRepository.findAllByCompetition(competition)) {
      teamStatsService.getTeamStatsCreateIfNotExist(team, ladder.getLadder());
    }

    return ladder;
  }

  public Optional<Ladder> getLadder(Competition competition, String ladderSlug) {
    return ladderRepository.findByCompetitionAndLadder(competition, ladderSlug);
  }

  public List<Ladder> getLaddersByCompetition(Competition competition) {
    return ladderRepository.findAllByCompetition(competition);
  }
}
