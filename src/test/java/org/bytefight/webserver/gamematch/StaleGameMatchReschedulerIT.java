package org.bytefight.webserver.gamematch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.application.StaleGameMatchRescheduler;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.infra.GameMatchProperties;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StaleGameMatchReschedulerIT extends FullStackIntegrationTestBase {
  @Autowired private StaleGameMatchRescheduler rescheduler;
  @Autowired private GameMatchService gameMatchService;
  @Autowired private GameMatchRepository gameMatchRepository;
  @Autowired private GameMatchProperties matchProperties;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private SubmissionRepository submissionRepository;
  @Autowired private FileRecordRepository fileRecordRepository;

  @Test
  void inProgressMatchPastThresholdIsNotRequeued() {
    GameMatch match = staleMatch(MatchStatus.in_progress, 1);

    rescheduler.rescheduleStaleGameMatches();

    GameMatch after = reload(match);
    assertThat(after.getStatus()).isEqualTo(MatchStatus.in_progress);
    assertThat(after.getTimesScheduled()).isEqualTo(1);
  }

  @Test
  void waitingMatchPastThresholdIsRequeuedOnce() {
    GameMatch match = staleMatch(MatchStatus.waiting, 1);

    rescheduler.rescheduleStaleGameMatches();

    GameMatch after = reload(match);
    // scheduleMatch re-enqueues it and bumps the counter; it returns to waiting with a fresh
    // scheduledAt (so it will not immediately re-trip the threshold).
    assertThat(after.getStatus()).isEqualTo(MatchStatus.waiting);
    assertThat(after.getTimesScheduled()).isEqualTo(2);
    assertThat(after.getScheduledAt()).isAfter(Instant.now().minus(1, ChronoUnit.MINUTES));
  }

  @Test
  void waitingMatchAtMaxReschedulesIsFailedNotRequeued() {
    GameMatch match = staleMatch(MatchStatus.waiting, matchProperties.getMaxReschedules());

    rescheduler.rescheduleStaleGameMatches();

    GameMatch after = reload(match);
    assertThat(after.getStatus()).isEqualTo(MatchStatus.failed);
    assertThat(after.getFinishedAt()).isNotNull();
  }

  @Test
  void sweepIsSkippedWhenRequeueStaleDisabled() {
    boolean original = matchProperties.isRequeueStale();
    matchProperties.setRequeueStale(false);
    try {
      GameMatch match = staleMatch(MatchStatus.waiting, 1);

      rescheduler.rescheduleStaleGameMatches();

      GameMatch after = reload(match);
      assertThat(after.getStatus()).isEqualTo(MatchStatus.waiting);
      assertThat(after.getTimesScheduled()).isEqualTo(1);
    } finally {
      matchProperties.setRequeueStale(original);
    }
  }

  private GameMatch staleMatch(MatchStatus status, int timesScheduled) {
    Competition competition = testDataFactory.createCompetition();
    String ladder = "ranked";
    testDataFactory.createLadder(competition, ladder);
    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    User user = testDataFactory.createUser();

    GameMatch match =
        gameMatchService.createMatch(
            user,
            teamA,
            teamB,
            createSubmission(teamA),
            createSubmission(teamB),
            ladder,
            MatchReason.matchmaking,
            null,
            null);
    match.setStatus(status);
    match.setTimesScheduled(timesScheduled);
    // Age it well past the stale threshold so the sweep picks it up.
    match.setScheduledAt(
        Instant.now().minus(matchProperties.getStaleThresholdMinutes() + 5, ChronoUnit.MINUTES));
    return gameMatchRepository.save(match);
  }

  private GameMatch reload(GameMatch match) {
    return gameMatchRepository.findById(match.getId()).orElseThrow();
  }

  private Submission createSubmission(Team team) {
    FileRecord record =
        FileRecord.builder()
            .uuid(UUID.randomUUID())
            .filename("bot.zip")
            .contentType("application/zip")
            .size(1L)
            .sha256("deadbeef")
            .storagePath("/tmp/bot.zip")
            .build();
    fileRecordRepository.save(record);

    Submission submission = new Submission();
    submission.setUuid(UUID.randomUUID());
    submission.setTeam(team);
    submission.setFileRecord(record);
    submission.setValidity(SubmissionValidity.valid);
    return submissionRepository.save(submission);
  }
}
