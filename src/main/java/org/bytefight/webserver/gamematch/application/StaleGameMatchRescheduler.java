package org.bytefight.webserver.gamematch.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.infra.GameMatchProperties;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaleGameMatchRescheduler {
  private final GameMatchService gameMatchService;
  private final GameMatchRepository gameMatchRepository;
  private final GameMatchProperties matchProperties;

  @Scheduled(fixedRate = 60000)
  @Transactional
  public void rescheduleStaleGameMatches() {
    if (!matchProperties.isRequeueStale()) {
      return;
    }

    // Only 'waiting' matches are swept: a match becomes 'in_progress' only once a worker has
    // emitted a "started" update, so requeuing one would clone live work. A wedged in_progress
    // match is the per-match watchdog's responsibility (cs3600_2026#35), not this sweep's.
    List<GameMatch> staleGameMatches =
        gameMatchRepository.findStaleWaitingMatches(
            Instant.now().minus(matchProperties.getStaleThresholdMinutes(), ChronoUnit.MINUTES));

    if (staleGameMatches.isEmpty()) {
      return;
    }

    int rescheduled = 0;
    int failed = 0;
    for (GameMatch gameMatch : staleGameMatches) {
      if (gameMatch.getTimesScheduled() >= matchProperties.getMaxReschedules()) {
        // Undeliverable: it has been scheduled the maximum number of times and is still waiting.
        // Fail it rather than requeue it forever. Guard on 'waiting' so a concurrent worker update
        // that moved it to in_progress/finished wins.
        int updated =
            gameMatchRepository.finalizeMatchResult(
                gameMatch.getUuid(),
                MatchStatus.failed,
                Instant.now(),
                List.of(MatchStatus.waiting));
        if (updated > 0) {
          failed++;
          log.warn(
              "Failing stale match {} after {} schedule attempts",
              gameMatch.getUuid(),
              gameMatch.getTimesScheduled());
        }
      } else {
        gameMatchService.scheduleMatch(gameMatch);
        rescheduled++;
      }
    }

    log.info("Stale sweep: rescheduled {}, failed {}", rescheduled, failed);
  }
}
