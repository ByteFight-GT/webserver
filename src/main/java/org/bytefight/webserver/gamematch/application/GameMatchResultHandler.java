package org.bytefight.webserver.gamematch.application;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchResult;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchUpdate;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.glicko.application.GlickoService;
import org.bytefight.webserver.rabbitmq.application.RabbitMQService;
import org.bytefight.webserver.submission.application.SubmissionService;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.tournament.application.TournamentResultHandler;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class GameMatchResultHandler {
  private final GameMatchService gameMatchService;
  private final GameMatchRepository gameMatchRepository;

  private final TeamService teamService;
  private final SubmissionService submissionService;
  private final RabbitMQService rabbitMQService;
  private final GlickoService glickoService;
  private final TournamentResultHandler tournamentResultHandler;

  /**
   * This method handles lightweight match status updates emitted by the engine
   *
   * @param gameMatchUpdate
   */
  public void handleGameMatchUpdate(GameMatchUpdate gameMatchUpdate) {
    GameMatch gameMatch =
        gameMatchService.getGameMatch(UUID.fromString(gameMatchUpdate.getUuid())).orElseThrow();

    if (gameMatchUpdate.isStarted() && gameMatch.getStatus() == MatchStatus.waiting) {
      gameMatch.setStatus(MatchStatus.in_progress);
      gameMatch.setStartedAt(Instant.now());
      gameMatchRepository.save(gameMatch);
    }
  }

  @Transactional
  public void handleGameMatchResult(GameMatchResult result) {
    UUID matchUuid = UUID.fromString(result.getUuid());
    Instant finishedAt = Instant.now();
    List<MatchStatus> allowedStatuses = List.of(MatchStatus.waiting, MatchStatus.in_progress);
    int updated =
        gameMatchRepository.finalizeMatchResult(
            matchUuid,
            result.getStatus(),
            finishedAt,
            result.getMapCode(),
            result.getOutcomeReasonCode(),
            allowedStatuses);
    if (updated == 0) {
      if (!gameMatchRepository.existsByUuid(matchUuid)) {
        throw new IllegalArgumentException("Game match not found");
      }
      throw new IllegalStateException(
          "Received an update for a match, but the match was not in a status which allows updates.");
    }

    GameMatch gameMatch =
        gameMatchService.getGameMatch(matchUuid).orElseThrow(() -> new IllegalStateException());
    if (gameMatch.getReason() == MatchReason.tournament) {
      tournamentResultHandler.handleTournamentResult(gameMatch, result.getStatus());
    }
    if (gameMatch.getReason() != MatchReason.validation) {
      glickoService.processGameMatchResult(gameMatch, false);
    } else {
      submissionService.onSubmissionValidationComplete(
          gameMatch.getSubmissionA(), result.getStatus() == MatchStatus.submission_valid);
    }
  }

  public List<GameMatchResult> deleteQueuedMatches() {
    List<GameMatchResult> removedResults = rabbitMQService.deleteGameResultQueue();
    return removedResults;
  }
}
