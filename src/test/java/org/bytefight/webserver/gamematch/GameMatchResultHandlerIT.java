package org.bytefight.webserver.gamematch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchResultHandler;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchResult;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class GameMatchResultHandlerIT extends FullStackIntegrationTestBase {
  @Autowired private GameMatchResultHandler gameMatchResultHandler;

  @Autowired private GameMatchService gameMatchService;

  @Autowired private GameMatchRepository gameMatchRepository;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private SubmissionRepository submissionRepository;

  @Autowired private FileRecordRepository fileRecordRepository;

  @Test
  void handleGameMatchResultIsIdempotent() {
    Competition competition =
        testDataFactory.createCompetition("comp-result", "Competition", true, 2);
    String ladder = "ladder1";
    testDataFactory.createLadder(competition, ladder);
    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    User user = testDataFactory.createUser();

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);

    GameMatch match =
        gameMatchService.createMatch(
            user,
            teamA,
            teamB,
            submissionA,
            submissionB,
            ladder,
            MatchReason.matchmaking,
            null,
            null);
    ReflectionTestUtils.setField(match, "status", MatchStatus.waiting);
    gameMatchRepository.save(match);

    UUID matchUuid = (UUID) ReflectionTestUtils.getField(match, "uuid");
    String uuid = matchUuid.toString();
    gameMatchResultHandler.handleGameMatchResult(createResult(uuid, MatchStatus.team_a_win));

    GameMatch afterFirst = gameMatchRepository.findByUuid(matchUuid).orElseThrow();
    MatchStatus firstStatus = (MatchStatus) ReflectionTestUtils.getField(afterFirst, "status");
    assertThat(firstStatus).isEqualTo(MatchStatus.team_a_win);
    assertThat(afterFirst.getMapCode()).isNull();
    assertThat(afterFirst.getOutcomeReasonCode()).isNull();

    assertThatThrownBy(
            () ->
                gameMatchResultHandler.handleGameMatchResult(
                    createResult(uuid, MatchStatus.team_b_win)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not in a status which allows updates");

    GameMatch afterSecond = gameMatchRepository.findByUuid(matchUuid).orElseThrow();
    MatchStatus secondStatus = (MatchStatus) ReflectionTestUtils.getField(afterSecond, "status");
    assertThat(secondStatus).isEqualTo(MatchStatus.team_a_win);
  }

  @Test
  void handleGameMatchResultStoresFinalMapAndOutcomeReason() {
    Competition competition =
        testDataFactory.createCompetition("comp-result-metadata", "Competition", true, 2);
    String ladder = "ladder1";
    testDataFactory.createLadder(competition, ladder);
    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    User user = testDataFactory.createUser();

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);
    GameMatch match =
        gameMatchService.createMatch(
            user,
            teamA,
            teamB,
            submissionA,
            submissionB,
            ladder,
            MatchReason.matchmaking,
            null,
            null);
    ReflectionTestUtils.setField(match, "status", MatchStatus.waiting);
    gameMatchRepository.save(match);

    UUID matchUuid = (UUID) ReflectionTestUtils.getField(match, "uuid");
    gameMatchResultHandler.handleGameMatchResult(
        createResult(
            matchUuid.toString(),
            MatchStatus.team_b_win,
            "arena_02",
            "engine_code_not_yet_registered"));

    GameMatch finishedMatch = gameMatchRepository.findByUuid(matchUuid).orElseThrow();
    assertThat(finishedMatch.getStatus()).isEqualTo(MatchStatus.team_b_win);
    assertThat(finishedMatch.getMapCode()).isEqualTo("arena_02");
    assertThat(finishedMatch.getOutcomeReasonCode()).isEqualTo("engine_code_not_yet_registered");
  }

  private GameMatchResult createResult(String uuid, MatchStatus status) {
    return new GameMatchResult(uuid, status);
  }

  private GameMatchResult createResult(
      String uuid, MatchStatus status, String mapCode, String outcomeReasonCode) {
    return new GameMatchResult(uuid, status, mapCode, outcomeReasonCode);
  }

  private Submission createSubmission(Team team) {
    FileRecord record = new FileRecord();
    ReflectionTestUtils.setField(record, "uuid", UUID.randomUUID());
    ReflectionTestUtils.setField(record, "filename", "bot.zip");
    ReflectionTestUtils.setField(record, "contentType", "application/zip");
    ReflectionTestUtils.setField(record, "size", 1L);
    ReflectionTestUtils.setField(record, "sha256", "deadbeef");
    ReflectionTestUtils.setField(record, "storagePath", "/tmp/bot.zip");
    fileRecordRepository.save(record);

    Submission submission = new Submission();
    ReflectionTestUtils.setField(submission, "uuid", UUID.randomUUID());
    ReflectionTestUtils.setField(submission, "team", team);
    ReflectionTestUtils.setField(submission, "fileRecord", record);
    return submissionRepository.save(submission);
  }
}
