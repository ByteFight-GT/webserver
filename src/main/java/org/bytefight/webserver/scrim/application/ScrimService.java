package org.bytefight.webserver.scrim.application;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.OptionalInt;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.scrim.domain.ScrimUsage;
import org.bytefight.webserver.scrim.domain.ScrimWindow;
import org.bytefight.webserver.scrim.infra.ScrimProperties;
import org.bytefight.webserver.scrim.infra.ScrimUsageRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScrimService {
  private final GameMatchService gameMatchService;
  private final ScrimUsageRepository scrimUsageRepository;
  private final ScrimProperties scrimProperties;
  private final Clock clock;

  /**
   * Debit one scrim unit and schedule one match, atomically. Throws {@link ScrimRejectedException}
   * (rolling back any partial debit) when the burst, daily, or weekly cap is hit.
   *
   * <p>Each call is its own transaction so that scheduling N-of-count leaves the granted matches and
   * their debits committed even when a later unit is rejected.
   */
  @Transactional
  public GameMatch scheduleOneOrThrow(User user, Team student, Team taBot, Competition competition) {
    if (gameMatchService.countTeamInFlightScrimMatches(student) >= scrimProperties.getBurst()) {
      throw new ScrimRejectedException(
          ScrimRejectedException.Reason.BURST, scrimProperties.getBurstRetryAfterSeconds());
    }

    Instant now = clock.instant();
    debitOrThrow(
        student, ScrimWindow.daily, scrimProperties.getDailyCap(), now,
        ScrimRejectedException.Reason.DAILY);
    debitOrThrow(
        student, ScrimWindow.weekly, scrimProperties.getWeeklyCap(), now,
        ScrimRejectedException.Reason.WEEKLY);

    GameMatch match =
        gameMatchService.createMatch(
            user,
            student, // initiatingTeam
            student, // teamA
            taBot, // teamB
            student.getCurrentSubmission(),
            taBot.getCurrentSubmission(),
            DefaultLadders.SCRIM,
            MatchReason.scrim,
            null,
            null);
    return gameMatchService.scheduleMatch(match);
  }

  private void debitOrThrow(
      Team student, ScrimWindow window, int cap, Instant now, ScrimRejectedException.Reason reason) {
    OptionalInt result =
        scrimUsageRepository.tryIncrement(
            student.getId(), window.name(), window.startOf(now), cap);
    if (result.isEmpty()) {
      long retryAfter = now.until(window.nextStartAfter(now), java.time.temporal.ChronoUnit.SECONDS);
      throw new ScrimRejectedException(reason, retryAfter);
    }
  }

  /** Remaining budget for a team in the current window (cap minus committed count). */
  public int remaining(Team student, ScrimWindow window, int cap) {
    return scrimUsageRepository
        .findByTeam_IdAndWindowKindAndWindowStart(
            student.getId(), window.name(), window.startOf(clock.instant()))
        .map(ScrimUsage::getCount)
        .map(count -> Math.max(0, cap - count))
        .orElse(cap);
  }

  public ScrimProperties getProperties() {
    return scrimProperties;
  }
}
