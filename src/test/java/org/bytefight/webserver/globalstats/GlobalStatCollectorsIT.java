package org.bytefight.webserver.globalstats;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.globalstats.application.GlobalStatsService;
import org.bytefight.webserver.globalstats.application.StatCollector;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/** Each test measures the change in a collector's value so it doesn't depend on existing rows. */
@Transactional
class GlobalStatCollectorsIT extends FullStackIntegrationTestBase {
  @Autowired private List<StatCollector> collectors;

  @Autowired private GlobalStatsService globalStatsService;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private SubmissionRepository submissionRepository;

  @Autowired private FileRecordRepository fileRecordRepository;

  @Autowired private GameMatchRepository gameMatchRepository;

  @Test
  void allExpectedCollectorsAreRegistered() {
    assertThat(collectors)
        .extracting(StatCollector::metric)
        .contains(
            "total_users", "total_teams", "total_matches_played", "student_code_bytes_uploaded");
  }

  @Test
  void totalUsersCountsUsers() {
    long before = collect("total_users");
    testDataFactory.createUser();
    testDataFactory.createUser();

    assertThat(collect("total_users")).isEqualTo(before + 2);
  }

  @Test
  void totalTeamsExcludesSoftDeletedTeams() {
    Competition competition = testDataFactory.createCompetition();
    long before = collect("total_teams");
    testDataFactory.createTeam(competition);
    testDataFactory.createTeam(competition);
    testDataFactory.createTeam(competition, UUID.randomUUID(), true);

    assertThat(collect("total_teams")).isEqualTo(before + 2);
  }

  @Test
  void totalMatchesPlayedCountsOnlyFinishedMatches() {
    Competition competition = testDataFactory.createCompetition();
    Team teamA = testDataFactory.createTeam(competition);
    Team teamB = testDataFactory.createTeam(competition);
    testDataFactory.createLadder(competition, "ladder1");
    Submission subA = createSubmission(teamA, 1L);
    Submission subB = createSubmission(teamB, 1L);
    long before = collect("total_matches_played");

    saveMatch(competition, teamA, teamB, subA, subB, MatchStatus.team_a_win);
    saveMatch(competition, teamA, teamB, subA, subB, MatchStatus.team_b_win);
    saveMatch(competition, teamA, teamB, subA, subB, MatchStatus.draw);
    saveMatch(competition, teamA, teamB, subA, subB, MatchStatus.scheduling);
    saveMatch(competition, teamA, teamB, subA, subB, MatchStatus.in_progress);
    saveMatch(competition, teamA, teamB, subA, subB, MatchStatus.failed);

    assertThat(collect("total_matches_played")).isEqualTo(before + 3);
  }

  @Test
  void studentCodeBytesUploadedSumsFileSizesIncludingDeletedSubmissions() {
    Competition competition = testDataFactory.createCompetition();
    Team team = testDataFactory.createTeam(competition);
    long before = collect("student_code_bytes_uploaded");

    createSubmission(team, 1000L);
    createSubmission(team, 250L);
    Submission deleted = createSubmission(team, 5000L);
    deleted.softDelete();
    submissionRepository.save(deleted);

    assertThat(collect("student_code_bytes_uploaded")).isEqualTo(before + 6250L);
  }

  @Test
  void refreshAllPersistsEveryCollectorValue() {
    testDataFactory.createUser();

    globalStatsService.refreshAll();

    Map<String, Long> stats = globalStatsService.getAllStats();
    assertThat(stats)
        .containsKeys(
            "total_users", "total_teams", "total_matches_played", "student_code_bytes_uploaded");
    assertThat(stats.get("total_users")).isEqualTo(collect("total_users"));
  }

  private long collect(String metric) {
    return collectors.stream()
        .filter(c -> c.metric().equals(metric))
        .findFirst()
        .orElseThrow()
        .collect();
  }

  private Submission createSubmission(Team team, long size) {
    FileRecord record = new FileRecord();
    ReflectionTestUtils.setField(record, "uuid", UUID.randomUUID());
    ReflectionTestUtils.setField(record, "filename", "bot.zip");
    ReflectionTestUtils.setField(record, "contentType", "application/zip");
    ReflectionTestUtils.setField(record, "size", size);
    ReflectionTestUtils.setField(record, "sha256", "deadbeef");
    ReflectionTestUtils.setField(record, "storagePath", "/tmp/bot.zip");
    fileRecordRepository.save(record);

    Submission submission = new Submission();
    ReflectionTestUtils.setField(submission, "uuid", UUID.randomUUID());
    ReflectionTestUtils.setField(submission, "team", team);
    ReflectionTestUtils.setField(submission, "fileRecord", record);
    ReflectionTestUtils.setField(submission, "validity", SubmissionValidity.valid);
    return submissionRepository.save(submission);
  }

  private void saveMatch(
      Competition competition,
      Team teamA,
      Team teamB,
      Submission submissionA,
      Submission submissionB,
      MatchStatus status) {
    GameMatch match = new GameMatch();
    ReflectionTestUtils.setField(match, "uuid", UUID.randomUUID());
    ReflectionTestUtils.setField(match, "competition", competition);
    ReflectionTestUtils.setField(match, "ladder", "ladder1");
    ReflectionTestUtils.setField(match, "teamA", teamA);
    ReflectionTestUtils.setField(match, "teamB", teamB);
    ReflectionTestUtils.setField(match, "submissionA", submissionA);
    ReflectionTestUtils.setField(match, "submissionB", submissionB);
    ReflectionTestUtils.setField(match, "matchSettings", Map.of("map", "arena_01"));
    ReflectionTestUtils.setField(match, "status", status);
    gameMatchRepository.save(match);
  }
}
