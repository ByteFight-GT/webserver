package org.bytefight.webserver.matchmaking.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledMatchMaker {
  private static final long POLLING_INTERVAL_SECONDS = 60;

  private final MatchmakingService matchmakingService;
  private final LadderRepository ladderRepository;

  @Scheduled(fixedRate = 60000)
  @Transactional
  public void runScheduledMatchmaking() {
    List<Ladder> enabledLadders =
        ladderRepository.findAllByScheduledMatchmakingEnabledTrueAndScheduledMatchmakingCronIsNotNull();

    Instant now = Instant.now();

    for (Ladder ladder : enabledLadders) {
      try {
        processLadder(ladder, now);
      } catch (Exception e) {
        log.error(
            "Error processing scheduled matchmaking for ladder {}: {}",
            ladder.getId(),
            e.getMessage(),
            e);
      }
    }
  }

  private void processLadder(Ladder ladder, Instant now) {
    Competition competition = ladder.getCompetition();

    if (!competition.isActive()) {
      return;
    }

    CronExpression cronExpression;
    try {
      cronExpression = CronExpression.parse(ladder.getScheduledMatchmakingCron());
    } catch (IllegalArgumentException e) {
      log.warn(
          "Invalid cron expression for ladder {}: {}",
          ladder.getId(),
          ladder.getScheduledMatchmakingCron());
      return;
    }

    Instant windowStart = now.minus(POLLING_INTERVAL_SECONDS, ChronoUnit.SECONDS);
    LocalDateTime windowStartLocal = LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC);
    LocalDateTime nextExecution = cronExpression.next(windowStartLocal);

    if (nextExecution == null) {
      return;
    }

    Instant nextExecutionInstant = nextExecution.toInstant(ZoneOffset.UTC);

    if (!nextExecutionInstant.isAfter(now)) {
      log.info(
          "Running scheduled matchmaking for ladder '{}' (competition '{}')",
          ladder.getLadder(),
          competition.getId());
      matchmakingService.createAndScheduleEvent(competition, ladder.getLadder());
    }
  }
}
