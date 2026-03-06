package org.bytefight.webserver.gamematch.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.infra.GameMatchProperties;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.matchmaking.application.MatchmakingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
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
    List<GameMatch> staleGameMatches = gameMatchRepository.findStaleWaitingMatches(Instant.now().minus(matchProperties.getStaleThresholdMinutes(), ChronoUnit.MINUTES));

    if(!staleGameMatches.isEmpty()) {
      log.info("Rescheduling {} stale game matches", staleGameMatches.size());
    }

    for(GameMatch gameMatch : staleGameMatches) {
      gameMatchService.scheduleMatch(gameMatch);
    }
  }
}
