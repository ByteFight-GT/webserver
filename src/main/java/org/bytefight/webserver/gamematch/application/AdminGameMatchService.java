package org.bytefight.webserver.gamematch.application;

import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.AdminCreateMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.AdminGameMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.AdminRescheduleResultDto;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminGameMatchService {
  private static final Set<MatchStatus> RESCHEDULABLE_STATUSES =
      EnumSet.of(
          MatchStatus.created, MatchStatus.scheduling, MatchStatus.waiting, MatchStatus.failed);

  private final CompetitionRepository competitionRepository;
  private final GameMatchRepository gameMatchRepository;
  private final LadderRepository ladderRepository;
  private final TeamRepository teamRepository;
  private final SubmissionRepository submissionRepository;
  private final GameMatchService gameMatchService;
  private final TransactionTemplate perMatchTransaction;

  public AdminGameMatchService(
      CompetitionRepository competitionRepository,
      GameMatchRepository gameMatchRepository,
      LadderRepository ladderRepository,
      TeamRepository teamRepository,
      SubmissionRepository submissionRepository,
      GameMatchService gameMatchService,
      PlatformTransactionManager transactionManager) {
    this.competitionRepository = competitionRepository;
    this.gameMatchRepository = gameMatchRepository;
    this.ladderRepository = ladderRepository;
    this.teamRepository = teamRepository;
    this.submissionRepository = submissionRepository;
    this.gameMatchService = gameMatchService;
    this.perMatchTransaction = new TransactionTemplate(transactionManager);
    this.perMatchTransaction.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public Page<AdminGameMatchDto> list(Specification<GameMatch> specs, Pageable pageable) {
    return gameMatchRepository.findAll(specs, pageable).map(AdminGameMatchDto::fromEntity);
  }

  public GameMatch createMatch(AdminCreateMatchDto input) {
    Competition competition =
        competitionRepository
            .findById(input.getCompetitionId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
    Ladder ladder =
        ladderRepository
            .findById(input.getLadderId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ladder not found"));
    if (!ladder.getCompetition().getId().equals(competition.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Ladder does not belong to competition");
    }

    Team teamA =
        teamRepository
            .findById(input.getTeamAId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team A not found"));
    Team teamB =
        teamRepository
            .findById(input.getTeamBId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team B not found"));
    if (!teamA.getCompetition().getId().equals(competition.getId())
        || !teamB.getCompetition().getId().equals(competition.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Teams do not belong to competition");
    }

    Submission submissionA =
        submissionRepository
            .findById(input.getTeamASubmissionId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Team A submission not found"));
    Submission submissionB =
        submissionRepository
            .findById(input.getTeamBSubmissionId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Team B submission not found"));
    if (!submissionA.getTeam().getId().equals(teamA.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Team A submission does not belong to Team A");
    }
    if (!submissionB.getTeam().getId().equals(teamB.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Team B submission does not belong to Team B");
    }

    GameMatch match =
        gameMatchService.createMatch(
            null,
            teamA,
            teamB,
            submissionA,
            submissionB,
            ladder.getLadder(),
            input.getReason(),
            null,
            null);

    return gameMatchService.scheduleMatch(match);
  }

  @Transactional(Transactional.TxType.NOT_SUPPORTED)
  public AdminRescheduleResultDto rescheduleMatches(List<Long> matchIds) {
    List<Long> rescheduled = new ArrayList<>();
    List<AdminRescheduleResultDto.Skipped> skipped = new ArrayList<>();

    // Dedupe so a repeated id can't enqueue the same match twice.
    for (Long id : new LinkedHashSet<>(matchIds)) {
      if (id == null) {
        skipped.add(new AdminRescheduleResultDto.Skipped(null, "not found"));
        continue;
      }
      try {
        Optional<String> skipReason = perMatchTransaction.execute(status -> rescheduleOne(id));
        if (skipReason == null || skipReason.isEmpty()) {
          rescheduled.add(id);
        } else {
          skipped.add(new AdminRescheduleResultDto.Skipped(id, skipReason.get()));
        }
      } catch (RuntimeException ex) {
        String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        skipped.add(new AdminRescheduleResultDto.Skipped(id, reason));
      }
    }

    return new AdminRescheduleResultDto(rescheduled, skipped);
  }

  /** Returns a skip reason, or empty if the match was scheduled. */
  private Optional<String> rescheduleOne(Long id) {
    GameMatch match = gameMatchRepository.findById(id).orElse(null);
    if (match == null) {
      return Optional.of("not found");
    }
    if (!RESCHEDULABLE_STATUSES.contains(match.getStatus())) {
      return Optional.of("status " + match.getStatus());
    }
    gameMatchService.scheduleMatch(match);
    return Optional.empty();
  }
}
