package org.bytefight.webserver.gamematch.application;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.gamematch.infra.GameMatchProperties;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.matchmaking.domain.MatchmakingEvent;
import org.bytefight.webserver.rabbitmq.application.RabbitMQService;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameMatchService {
  private static final String TOURNAMENT_LADDER = "tournament";

  private final GameMatchRepository gameMatchRepository;
  private final TeamRepository teamRepository;
  private final RabbitMQService rabbitMQService;
  private final GameMatchProperties gameMatchProperties;
  private final Clock clock;

  public Optional<GameMatch> getGameMatch(UUID id) {
    return gameMatchRepository.findByUuid(id);
  }

  public GameMatch createMatch(
      User creatingUser,
      Team teamA,
      Team teamB,
      Submission submissionA,
      Submission submissionB,
      String ladder,
      MatchReason reason,
      Map<String, Object> matchSettings,
      MatchmakingEvent matchmakingEvent) {
    return createMatch(
        creatingUser,
        null,
        teamA,
        teamB,
        submissionA,
        submissionB,
        ladder,
        reason,
        matchSettings,
        matchmakingEvent);
  }

  public GameMatch createMatch(
      User creatingUser,
      Team initiatingTeam,
      Team teamA,
      Team teamB,
      Submission submissionA,
      Submission submissionB,
      String ladder,
      MatchReason reason,
      Map<String, Object> matchSettings,
      MatchmakingEvent matchmakingEvent) {
    if (teamA == null || teamB == null) {
      throw new IllegalArgumentException("teamA and teamB are required");
    }

    if (submissionA == null || submissionB == null) {
      throw new IllegalArgumentException("submissionA and submissionB are required");
    }

    if (!teamA.getCompetition().equals(teamB.getCompetition())) {
      throw new IllegalArgumentException("Both teams must be from the same competition");
    }

    GameMatch gameMatch = new GameMatch();
    gameMatch.setUuid(UUID.randomUUID());
    gameMatch.setCompetition(teamA.getCompetition());
    gameMatch.setTeamA(teamA);
    gameMatch.setTeamB(teamB);
    gameMatch.setSubmissionA(submissionA);
    gameMatch.setSubmissionB(submissionB);
    gameMatch.setStatus(MatchStatus.created);
    gameMatch.setLadder(ladder.trim().toLowerCase());
    gameMatch.setReason(reason);
    gameMatch.setMatchmakingEvent(matchmakingEvent);
    gameMatch.setInitiatingTeam(initiatingTeam);

    gameMatch.setMatchSettings(Objects.requireNonNullElse(matchSettings, Collections.emptyMap()));

    gameMatch.setCreatedByUser(creatingUser);
    gameMatch.setUpdatedByUser(creatingUser);

    gameMatchRepository.save(gameMatch);

    return gameMatch;
  }

  public GameMatch scheduleMatch(GameMatch match) {
    if (!match.getCompetition().isActive()) {
      throw new IllegalArgumentException("Competition is not active");
    }

    GameMatchJob job = GameMatchJob.from(match);
    try {
      rabbitMQService.enqueueGameMatchJob(job);
    } catch (RuntimeException ex) {
      throw new IllegalStateException("Failed to enqueue game match job", ex);
    }

    match.setStatus(MatchStatus.waiting);
    match.setScheduledAt(Instant.now());
    match.setTimesScheduled(match.getTimesScheduled() + 1);

    return gameMatchRepository.save(match);
  }

  public Page<GameMatch> getPaginatedMatches(
      Competition competition,
      String ladderSlug,
      String teamUuid,
      String opponentTeamName,
      String teamWin,
      String initiatingTeamUuid,
      String notInitiatingTeamUuid,
      String submissionUuid,
      MatchReason matchReason,
      MatchStatus matchStatus,
      PageRequest page) {
    TeamWinFilter teamWinFilter = TeamWinFilter.from(teamWin);
    UUID teamId = parseOptionalUuid(teamUuid, "teamUuid");
    boolean hasOpponentTeamName = opponentTeamName != null && !opponentTeamName.isBlank();
    UUID opponentTeamId = resolveOpponentTeamUuid(competition, opponentTeamName).orElse(null);
    UUID initiatingTeamId = parseOptionalUuid(initiatingTeamUuid, "initiatingTeamUuid");
    UUID notInitiatingTeamId = parseOptionalUuid(notInitiatingTeamUuid, "notInitiatingTeamUuid");
    UUID submissionId = parseOptionalUuid(submissionUuid, "submissionUuid");

    if (teamId == null && teamWinFilter != TeamWinFilter.ANY) {
      throw new IllegalArgumentException("teamUuid is required when teamWin is Win or Lose");
    }
    if (hasOpponentTeamName && opponentTeamId == null) {
      return Page.empty(page);
    }

    Specification<GameMatch> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();

          predicates.add(cb.notEqual(root.get("ladder"), TOURNAMENT_LADDER));

          if (competition != null) {
            predicates.add(cb.equal(root.get("competition"), competition));
          }

          if (ladderSlug != null) {
            predicates.add(cb.equal(root.get("ladder"), ladderSlug));
          }

          if (matchReason != null) {
            predicates.add(cb.equal(root.get("reason"), matchReason));
          }

          if (matchStatus != null) {
            predicates.add(cb.equal(root.get("status"), matchStatus));
          }

          if (teamId != null) {
            predicates.add(
                cb.or(
                    cb.equal(root.get("teamA").get("uuid"), teamId),
                    cb.equal(root.get("teamB").get("uuid"), teamId)));
          }

          if (opponentTeamId != null) {
            predicates.add(
                cb.or(
                    cb.equal(root.get("teamA").get("uuid"), opponentTeamId),
                    cb.equal(root.get("teamB").get("uuid"), opponentTeamId)));
          }

          if (teamWinFilter == TeamWinFilter.WIN) {
            predicates.add(
                cb.or(
                    cb.and(
                        cb.equal(root.get("teamA").get("uuid"), teamId),
                        cb.equal(root.get("status"), MatchStatus.team_a_win)),
                    cb.and(
                        cb.equal(root.get("teamB").get("uuid"), teamId),
                        cb.equal(root.get("status"), MatchStatus.team_b_win))));
          } else if (teamWinFilter == TeamWinFilter.LOSE) {
            predicates.add(
                cb.or(
                    cb.and(
                        cb.equal(root.get("teamA").get("uuid"), teamId),
                        cb.equal(root.get("status"), MatchStatus.team_b_win)),
                    cb.and(
                        cb.equal(root.get("teamB").get("uuid"), teamId),
                        cb.equal(root.get("status"), MatchStatus.team_a_win))));
          }

          if (initiatingTeamId != null) {
            predicates.add(cb.equal(root.get("initiatingTeam").get("uuid"), initiatingTeamId));
          }

          if (notInitiatingTeamId != null) {
            predicates.add(
                cb.or(
                    cb.isNull(root.get("initiatingTeam")),
                    cb.notEqual(root.get("initiatingTeam").get("uuid"), notInitiatingTeamId)));
          }

          if (submissionId != null) {
            predicates.add(
                cb.or(
                    cb.equal(root.get("submissionA").get("uuid"), submissionId),
                    cb.equal(root.get("submissionB").get("uuid"), submissionId)));
          }

          return predicates.isEmpty()
              ? cb.conjunction()
              : cb.and(predicates.toArray(new Predicate[0]));
        };

    return gameMatchRepository.findAll(spec, page);
  }

  private Optional<UUID> resolveOpponentTeamUuid(Competition competition, String opponentTeamName) {
    if (opponentTeamName == null || opponentTeamName.isBlank()) {
      return Optional.empty();
    }
    if (competition == null) {
      throw new IllegalArgumentException(
          "competition is required when filtering by opponentTeamName");
    }

    String normalizedName = opponentTeamName.trim().toLowerCase(Locale.ROOT);
    return teamRepository
        .findByCompetitionAndNameNormalizedAndDeletedAtNull(competition, normalizedName)
        .map(Team::getUuid);
  }

  private UUID parseOptionalUuid(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(fieldName + " must be a valid UUID", ex);
    }
  }

  private enum TeamWinFilter {
    ANY,
    WIN,
    LOSE;

    private static TeamWinFilter from(String teamWin) {
      if (teamWin == null || teamWin.isBlank()) {
        return ANY;
      }

      return switch (teamWin.trim().toLowerCase(Locale.ROOT)) {
        case "any" -> ANY;
        case "win" -> WIN;
        case "lose" -> LOSE;
        default -> throw new IllegalArgumentException("teamWin must be one of Any, Win, Lose");
      };
    }
  }

  public Page<GameMatch> getFullPaginatedQueue(Competition competition, Pageable page) {
    return gameMatchRepository.findByCompetitionAndStatus(
        competition,
        Set.of(
            MatchStatus.created,
            MatchStatus.scheduling,
            MatchStatus.waiting,
            MatchStatus.in_progress),
        TOURNAMENT_LADDER,
        page);
  }

  public Optional<GameMatchDto> getDTOByUuid(String uuid) {
    Optional<GameMatch> dto = gameMatchRepository.findByUuid(UUID.fromString(uuid));
    if (dto.isEmpty()) return Optional.empty();

    return Optional.of(GameMatchDto.fromEntity(dto.get()));
  }

  public long countTeamQueuedMatchesByLadder(Team team, Ladder ladder) {
    return gameMatchRepository.countTeamMatchesByLadderAndStatus(
        team, ladder.getLadder(), Set.of(MatchStatus.waiting, MatchStatus.in_progress));
  }

  /**
   * Count a team's validation matches that are still in flight. Includes {@code created} and {@code
   * scheduling}, not just {@code waiting}/{@code in_progress}, so a match that has been created but
   * not yet enqueued still counts against the cap (a submission rush would otherwise slip through
   * that window).
   */
  public long countTeamInFlightValidationMatches(Team team) {
    // Validation matches are created with teamA == teamB == the submitting team and no initiating
    // team, so count by teamA (the shared countTeamMatchesByLadderAndStatus keys on initiatingTeam,
    // which is null here).
    return gameMatchRepository.countTeamAMatchesByLadderAndStatus(
        team,
        DefaultLadders.VALIDATION,
        Set.of(
            MatchStatus.created,
            MatchStatus.scheduling,
            MatchStatus.waiting,
            MatchStatus.in_progress));
  }
}
