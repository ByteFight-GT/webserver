package org.bytefight.webserver.glicko;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.glicko.application.GlickoService;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.infra.TeamGlickoHistoryRepository;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
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

@Transactional
class GlickoServiceIT extends FullStackIntegrationTestBase {
  private static final String LADDER_SLUG = "ladder1";

  @Autowired private GlickoService glickoService;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private LadderRepository ladderRepository;

  @Autowired private TeamStatsRepository teamStatsRepository;

  @Autowired private TeamGlickoHistoryRepository teamGlickoHistoryRepository;

  @Autowired private SubmissionRepository submissionRepository;

  @Autowired private FileRecordRepository fileRecordRepository;

  @Autowired private GameMatchRepository gameMatchRepository;

  @Test
  void processGameMatchResultUpdatesTeamStatsAndHistory() {
    Competition competition =
        testDataFactory.createCompetition("glicko-comp", "Glicko Competition", true, 2);
    Ladder ladder = createLadder(competition, LADDER_SLUG);
    ladderRepository.save(ladder);

    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);

    GameMatch match =
        createMatch(
            competition,
            teamA,
            teamB,
            submissionA,
            submissionB,
            LADDER_SLUG,
            MatchStatus.team_a_win);
    gameMatchRepository.save(match);

    glickoService.processGameMatchResult(match, false);

    Optional<TeamStats> teamAStats = teamStatsRepository.findByTeamAndLadder(teamA, LADDER_SLUG);
    Optional<TeamStats> teamBStats = teamStatsRepository.findByTeamAndLadder(teamB, LADDER_SLUG);

    assertThat(teamAStats).isPresent();
    assertThat(teamBStats).isPresent();

    assertThat(getIntField(teamAStats.get(), "matchesPlayed")).isEqualTo(1);
    assertThat(getIntField(teamAStats.get(), "wins")).isEqualTo(1);
    assertThat(getIntField(teamAStats.get(), "losses")).isEqualTo(0);
    assertThat(getIntField(teamAStats.get(), "draws")).isEqualTo(0);

    assertThat(getIntField(teamBStats.get(), "matchesPlayed")).isEqualTo(1);
    assertThat(getIntField(teamBStats.get(), "wins")).isEqualTo(0);
    assertThat(getIntField(teamBStats.get(), "losses")).isEqualTo(1);
    assertThat(getIntField(teamBStats.get(), "draws")).isEqualTo(0);

    assertThat(teamGlickoHistoryRepository.count()).isEqualTo(2);
  }

  @Test
  void processGameMatchResultCalculatesExpectedRatings() {
    Competition competition =
        testDataFactory.createCompetition("glicko-calc", "Glicko Calc", true, 2);
    Ladder ladder = createLadder(competition, LADDER_SLUG);
    ladderRepository.save(ladder);

    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);

    GameMatch match =
        createMatch(
            competition,
            teamA,
            teamB,
            submissionA,
            submissionB,
            LADDER_SLUG,
            MatchStatus.team_a_win);
    gameMatchRepository.save(match);

    glickoService.processGameMatchResult(match, false);

    TeamStats teamAStats =
        teamStatsRepository.findByTeamAndLadder(teamA, LADDER_SLUG).orElseThrow();
    TeamStats teamBStats =
        teamStatsRepository.findByTeamAndLadder(teamB, LADDER_SLUG).orElseThrow();

    double teamARating = getDoubleField(teamAStats, "glickoRating");
    double teamARD = getDoubleField(teamAStats, "glickoRd");
    double teamAVolatility = getDoubleField(teamAStats, "glickoVolatility");

    double teamBRating = getDoubleField(teamBStats, "glickoRating");
    double teamBRD = getDoubleField(teamBStats, "glickoRd");
    double teamBVolatility = getDoubleField(teamBStats, "glickoVolatility");

    assertThat(teamARating).isCloseTo(1662.25, within(0.5));
    assertThat(teamARD).isCloseTo(290.28, within(0.5));
    assertThat(teamAVolatility).isCloseTo(0.06, within(0.0001));

    assertThat(teamBRating).isCloseTo(1337.75, within(0.5));
    assertThat(teamBRD).isCloseTo(290.28, within(0.5));
    assertThat(teamBVolatility).isCloseTo(0.06, within(0.0001));
  }

  @Test
  void processTwoMatchesAcrossThreeTeamsCalculatesExpectedRatings() {
    Competition competition =
        testDataFactory.createCompetition("glicko-3teams", "Glicko 3 Teams", true, 2);
    Ladder ladder = createLadder(competition, LADDER_SLUG);
    ladderRepository.save(ladder);

    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamC = testDataFactory.createTeam(competition, UUID.randomUUID(), false);

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);
    Submission submissionC = createSubmission(teamC);

    GameMatch match1 =
        createMatch(
            competition,
            teamA,
            teamB,
            submissionA,
            submissionB,
            LADDER_SLUG,
            MatchStatus.team_a_win);
    gameMatchRepository.save(match1);
    glickoService.processGameMatchResult(match1, false);

    GameMatch match2 =
        createMatch(
            competition,
            teamA,
            teamC,
            submissionA,
            submissionC,
            LADDER_SLUG,
            MatchStatus.team_b_win);
    gameMatchRepository.save(match2);
    glickoService.processGameMatchResult(match2, false);

    TeamStats teamAStats =
        teamStatsRepository.findByTeamAndLadder(teamA, LADDER_SLUG).orElseThrow();
    TeamStats teamBStats =
        teamStatsRepository.findByTeamAndLadder(teamB, LADDER_SLUG).orElseThrow();
    TeamStats teamCStats =
        teamStatsRepository.findByTeamAndLadder(teamC, LADDER_SLUG).orElseThrow();

    assertThat(getIntField(teamAStats, "matchesPlayed")).isEqualTo(2);
    assertThat(getIntField(teamAStats, "wins")).isEqualTo(1);
    assertThat(getIntField(teamAStats, "losses")).isEqualTo(1);
    assertThat(getIntField(teamAStats, "draws")).isEqualTo(0);

    assertThat(getIntField(teamBStats, "matchesPlayed")).isEqualTo(1);
    assertThat(getIntField(teamBStats, "wins")).isEqualTo(0);
    assertThat(getIntField(teamBStats, "losses")).isEqualTo(1);
    assertThat(getIntField(teamBStats, "draws")).isEqualTo(0);

    assertThat(getIntField(teamCStats, "matchesPlayed")).isEqualTo(1);
    assertThat(getIntField(teamCStats, "wins")).isEqualTo(1);
    assertThat(getIntField(teamCStats, "losses")).isEqualTo(0);
    assertThat(getIntField(teamCStats, "draws")).isEqualTo(0);

    assertThat(getDoubleField(teamAStats, "glickoRating")).isCloseTo(1497.45, within(0.5));
    assertThat(getDoubleField(teamAStats, "glickoRd")).isCloseTo(256.35, within(0.5));
    assertThat(getDoubleField(teamAStats, "glickoVolatility")).isCloseTo(0.06, within(0.0001));

    assertThat(getDoubleField(teamBStats, "glickoRating")).isCloseTo(1337.69, within(0.5));
    assertThat(getDoubleField(teamBStats, "glickoRd")).isCloseTo(290.32, within(0.5));
    assertThat(getDoubleField(teamBStats, "glickoVolatility")).isCloseTo(0.06, within(0.0001));

    assertThat(getDoubleField(teamCStats, "glickoRating")).isCloseTo(1731.88, within(0.5));
    assertThat(getDoubleField(teamCStats, "glickoRd")).isCloseTo(286.93, within(0.5));
    assertThat(getDoubleField(teamCStats, "glickoVolatility")).isCloseTo(0.06, within(0.0001));

    assertThat(teamGlickoHistoryRepository.count()).isEqualTo(4);
  }

  private Ladder createLadder(Competition competition, String ladderSlug) {
    Ladder ladder = new Ladder();
    ReflectionTestUtils.setField(ladder, "competition", competition);
    ReflectionTestUtils.setField(ladder, "ladder", ladderSlug);
    ReflectionTestUtils.setField(ladder, "glickoDefaultRating", 1500.0);
    ReflectionTestUtils.setField(ladder, "glickoDefaultRd", 350.0);
    ReflectionTestUtils.setField(ladder, "glickoRdMax", 350.0);
    ReflectionTestUtils.setField(ladder, "glickoRdMin", 30.0);
    ReflectionTestUtils.setField(ladder, "glickoPhiInflationPerDay", 0.0);
    ReflectionTestUtils.setField(ladder, "glickoTau", 0.5);
    ReflectionTestUtils.setField(ladder, "glickoSigmaDefault", 0.06);
    ReflectionTestUtils.setField(ladder, "glickoSigmaMin", 0.03);
    ReflectionTestUtils.setField(ladder, "glickoSigmaMax", 0.2);
    return ladder;
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
    ReflectionTestUtils.setField(submission, "validity", SubmissionValidity.valid);
    return submissionRepository.save(submission);
  }

  private GameMatch createMatch(
      Competition competition,
      Team teamA,
      Team teamB,
      Submission submissionA,
      Submission submissionB,
      String ladder,
      MatchStatus status) {
    GameMatch match = new GameMatch();
    ReflectionTestUtils.setField(match, "uuid", UUID.randomUUID());
    ReflectionTestUtils.setField(match, "competition", competition);
    ReflectionTestUtils.setField(match, "ladder", ladder);
    ReflectionTestUtils.setField(match, "teamA", teamA);
    ReflectionTestUtils.setField(match, "teamB", teamB);
    ReflectionTestUtils.setField(match, "submissionA", submissionA);
    ReflectionTestUtils.setField(match, "submissionB", submissionB);
    ReflectionTestUtils.setField(match, "matchSettings", Map.of("map", "arena_01"));
    ReflectionTestUtils.setField(match, "status", status);
    return match;
  }

  private int getIntField(TeamStats stats, String field) {
    Object value = ReflectionTestUtils.getField(stats, field);
    return value == null ? 0 : (int) value;
  }

  private double getDoubleField(TeamStats stats, String field) {
    Object value = ReflectionTestUtils.getField(stats, field);
    return value == null ? 0.0 : (double) value;
  }
}
