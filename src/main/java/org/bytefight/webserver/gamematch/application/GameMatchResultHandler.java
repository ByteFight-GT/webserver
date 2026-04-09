package org.bytefight.webserver.gamematch.application;

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

  public void handleGameMatchResult(GameMatchResult result) {
    GameMatch gameMatch =
        gameMatchService
            .getGameMatch(UUID.fromString(result.getUuid()))
            .orElseThrow(() -> new IllegalArgumentException("Game match not found"));

    gameMatch.setStatus(result.getStatus());
    gameMatch.setFinishedAt(Instant.now());
    gameMatchRepository.save(gameMatch);
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
