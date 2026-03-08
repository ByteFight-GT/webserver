package org.bytefight.webserver.ladder.application;

import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.domain.dto.AdminCreateLadderDto;
import org.bytefight.webserver.ladder.domain.dto.AdminUpdateLadderDto;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminLadderService {
  private static final double DEFAULT_GLICKO_DEFAULT_RATING = 1500.0;
  private static final double DEFAULT_GLICKO_DEFAULT_RD = 350.0;
  private static final double DEFAULT_GLICKO_RD_MAX = 350.0;
  private static final double DEFAULT_GLICKO_RD_MIN = 30.0;
  private static final double DEFAULT_GLICKO_PHI_INFLATION_PER_DAY = 0.0;
  private static final double DEFAULT_GLICKO_TAU = 0.5;
  private static final double DEFAULT_GLICKO_SIGMA_DEFAULT = 0.06;
  private static final double DEFAULT_GLICKO_SIGMA_MIN = 0.03;
  private static final double DEFAULT_GLICKO_SIGMA_MAX = 0.2;

  private final CompetitionRepository competitionRepository;
  private final LadderService ladderService;
  private final LadderRepository ladderRepository;

  public Page<Ladder> listByCompetitionId(Long competitionId, Pageable pageable) {
    return ladderRepository.findByCompetitionId(competitionId, pageable);
  }

  public Page<Ladder> listLadders(Specification<Ladder> specification, Pageable pageable) {
    return ladderRepository.findAll(specification, pageable);
  }

  public Ladder createLadder(AdminCreateLadderDto input) {
    Competition competition =
        competitionRepository
            .findById(input.getCompetitionId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));

    return ladderService.createLadder(
        competition,
        input.getLadder(),
        valueOrDefault(input.getGlickoDefaultRating(), DEFAULT_GLICKO_DEFAULT_RATING),
        valueOrDefault(input.getGlickoDefaultRd(), DEFAULT_GLICKO_DEFAULT_RD),
        valueOrDefault(input.getGlickoRdMax(), DEFAULT_GLICKO_RD_MAX),
        valueOrDefault(input.getGlickoRdMin(), DEFAULT_GLICKO_RD_MIN),
        valueOrDefault(input.getGlickoPhiInflationPerDay(), DEFAULT_GLICKO_PHI_INFLATION_PER_DAY),
        valueOrDefault(input.getGlickoTau(), DEFAULT_GLICKO_TAU),
        valueOrDefault(input.getGlickoSigmaDefault(), DEFAULT_GLICKO_SIGMA_DEFAULT),
        valueOrDefault(input.getGlickoSigmaMin(), DEFAULT_GLICKO_SIGMA_MIN),
        valueOrDefault(input.getGlickoSigmaMax(), DEFAULT_GLICKO_SIGMA_MAX));
  }

  public Ladder updateLadder(Long id, AdminUpdateLadderDto input) {
    Ladder ladder =
        ladderRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ladder not found"));

    if (input.getGlickoDefaultRating() != null) {
      ladder.setGlickoDefaultRating(input.getGlickoDefaultRating());
    }
    if (input.getGlickoDefaultRd() != null) {
      ladder.setGlickoDefaultRd(input.getGlickoDefaultRd());
    }
    if (input.getGlickoRdMax() != null) {
      ladder.setGlickoRdMax(input.getGlickoRdMax());
    }
    if (input.getGlickoRdMin() != null) {
      ladder.setGlickoRdMin(input.getGlickoRdMin());
    }
    if (input.getGlickoPhiInflationPerDay() != null) {
      ladder.setGlickoPhiInflationPerDay(input.getGlickoPhiInflationPerDay());
    }
    if (input.getGlickoTau() != null) {
      ladder.setGlickoTau(input.getGlickoTau());
    }
    if (input.getGlickoSigmaDefault() != null) {
      ladder.setGlickoSigmaDefault(input.getGlickoSigmaDefault());
    }
    if (input.getGlickoSigmaMin() != null) {
      ladder.setGlickoSigmaMin(input.getGlickoSigmaMin());
    }
    if (input.getGlickoSigmaMax() != null) {
      ladder.setGlickoSigmaMax(input.getGlickoSigmaMax());
    }
    if (input.getAllowUserMatches() != null) {
      ladder.setAllowUserMatches(input.getAllowUserMatches());
    }
    if (input.getScheduledMatchmakingEnabled() != null) {
      ladder.setScheduledMatchmakingEnabled(input.getScheduledMatchmakingEnabled());
    }

    if (input.getScheduledMatchmakingCron() != null) {
      ladder.setScheduledMatchmakingCron(input.getScheduledMatchmakingCron());
    }

    return ladderRepository.save(ladder);
  }

  public Ladder getLadder(Long id) {
    return ladderRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ladder not found"));
  }

  private static double valueOrDefault(Double value, double fallback) {
    return value == null ? fallback : value;
  }
}
