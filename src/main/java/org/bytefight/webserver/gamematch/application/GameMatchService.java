package org.bytefight.webserver.gamematch.application;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameMatch;
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

  private final GameMatchRepository gameMatchRepository;
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

    return gameMatchRepository.save(match);
  }

  public Page<GameMatch> getPaginatedMatches(
      Competition competition,
      String ladderSlug,
      String teamUuid,
      String initiatingTeamUuid,
      String notInitiatingTeamUuid,
      String submissionUuid,
      MatchReason matchReason,
      MatchStatus matchStatus,
      PageRequest page) {
    Specification<GameMatch> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();

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

          if (teamUuid != null) {
            UUID teamId = UUID.fromString(teamUuid);
            predicates.add(
                cb.or(
                    cb.equal(root.get("teamA").get("uuid"), teamId),
                    cb.equal(root.get("teamB").get("uuid"), teamId)));
          }

          if (initiatingTeamUuid != null) {
            predicates.add(
                cb.equal(
                    root.get("initiatingTeam").get("uuid"), UUID.fromString(initiatingTeamUuid)));
          }

          if (notInitiatingTeamUuid != null) {
            UUID notInitiatingTeamId = UUID.fromString(notInitiatingTeamUuid);
            predicates.add(
                cb.or(
                    cb.isNull(root.get("initiatingTeam")),
                    cb.notEqual(root.get("initiatingTeam").get("uuid"), notInitiatingTeamId)));
          }

          if (submissionUuid != null) {
            UUID submissionId = UUID.fromString(submissionUuid);
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

  public Page<GameMatch> getFullPaginatedQueue(Competition competition, Pageable page) {
    return gameMatchRepository.findByCompetitionAndStatus(
        competition,
        Set.of(
            MatchStatus.created,
            MatchStatus.scheduling,
            MatchStatus.waiting,
            MatchStatus.in_progress),
        page);
  }

  public GameMatchDto getDTOById(Long id) {
    return GameMatchDto.fromEntity(gameMatchRepository.getReferenceById(id));
  }

  public Optional<GameMatchDto> getDTOByUuid(String uuid) {
    Optional<GameMatch> dto = gameMatchRepository.findByUuid(UUID.fromString(uuid));
    if (dto.isEmpty()) return Optional.empty();

    return Optional.of(GameMatchDto.fromEntity(dto.get()));
  }

  public GameMatch getReferenceById(Long id) {
    return gameMatchRepository.getReferenceById(id);
  }

  public Optional<GameMatch> getReferenceByUuid(String uuid) {
    return gameMatchRepository.findByUuid(UUID.fromString(uuid));
  }

  public boolean isGameMatchIdExist(Long id) {
    return gameMatchRepository.existsById(id);
  }

  public boolean isGameMatchUuidExist(String uuid) {
    return gameMatchRepository.existsByUuid(UUID.fromString(uuid));
  }

  public boolean isGameMatchWaiting(String uuid) {
    return false;
    //        return gameMatchRepository.findByUuid(UUID.fromString(uuid)).orElseThrow().getStatus()
    // == MatchStatus.WAITING;
  }

  public List<GameMatch> getFailedMatches() {
    return null;
    //        return gameMatchRepository
    //                .findByStatus(MatchStatus.FAILED)
    //                .stream()
    //                .toList();
  }

  @Transactional
  public void rescheduleStaleMatches(boolean isIgnoreLimit) {
    LocalDateTime thresholdTime =
        LocalDateTime.now(clock).minusMinutes(gameMatchProperties.getStaleThresholdMinutes());

    // This atomically marks all stale matches as RESCHEDULING and returns their ids
    List<Long> matchesToReschedule = gameMatchRepository.claimAndMarkStaleMatches(thresholdTime);
    log.info("Found {} matches to reschedule", matchesToReschedule.size());
    matchesToReschedule.forEach(id -> rescheduleMatch(id, isIgnoreLimit));
    log.info("Rescheduling completed");
  }

  @Transactional
  public void adminRescheduleMatches(List<Long> matchIds) {
    log.info("Admin {} matches to reschedule", matchIds.size());
    matchIds.forEach(id -> rescheduleMatch(id, true));
    log.info("Rescheduling completed");
  }

  @Transactional
  public GameMatchJob rescheduleMatch(Long gameMatchId, boolean isIgnoreLimit) {
    GameMatch gameMatch = gameMatchRepository.getReferenceById(gameMatchId);
    //        Integer timesQueued = gameMatch.getTimesQueued();
    //        if (!isIgnoreLimit && timesQueued == 3) {
    //            throw new IllegalStateException("Match " + gameMatch.getId() + " has exceeded
    // maximum retry attempts (3)");
    //        }
    //
    //        if(gameMatch.getStatus() != MatchStatus.WAITING) {
    //            throw new IllegalArgumentException("Match " + gameMatch.getId() + " cannot be
    // rescheduled.");
    //        }
    //
    //        gameMatch.setQueuedAt(LocalDateTime.now(clock));
    //        gameMatch.incrementTimesQueued();
    //        gameMatch.setStatus(MatchStatus.WAITING);
    GameMatchJob job = GameMatchJob.from(gameMatch);
    gameMatchRepository.save(gameMatch);
    rabbitMQService.enqueueGameMatchJob(job);
    log.info("rescheduled match {}", job);
    return job;
  }

  public long countTeamQueuedMatchesByLadder(Team team, Ladder ladder) {
    return gameMatchRepository.countTeamMatchesByLadderAndStatus(
        team, ladder.getLadder(), Set.of(MatchStatus.waiting, MatchStatus.in_progress));
  }
}
