package org.bytefight.webserver.gamematch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class GameMatchServiceIT extends FullStackIntegrationTestBase {
  @Autowired private GameMatchService gameMatchService;

  @Autowired private GameMatchRepository gameMatchRepository;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private SubmissionRepository submissionRepository;

  @Autowired private FileRecordRepository fileRecordRepository;

  @Autowired private RabbitTemplate rabbitTemplate;

  @Autowired private TopicExchange gameMatchExchange;

  @Autowired private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

  @Test
  void createAndScheduleMatchEnqueuesByCompetitionAndLadder() {
    Competition competition = testDataFactory.createCompetition("comp", "Competition", true, 2);
    String ladder = "ladder1";
    testDataFactory.createLadder(competition, ladder);
    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    User user = testDataFactory.createUser();

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);

    String routingKey = "competition." + competition.getSlug() + "." + ladder;

    Queue queue = QueueBuilder.nonDurable("test.match.queue").autoDelete().build();
    RabbitAdmin admin = new RabbitAdmin(connectionFactory);
    admin.declareExchange(gameMatchExchange);
    admin.declareQueue(queue);
    Binding binding = BindingBuilder.bind(queue).to(gameMatchExchange).with(routingKey);
    admin.declareBinding(binding);

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
    match.setMatchSettings(Map.of("map", "arena_01"));

    gameMatchService.scheduleMatch(match);

    Object message = rabbitTemplate.receiveAndConvert(queue.getName(), 2000);
    assertThat(message).isInstanceOf(GameMatchJob.class);

    GameMatchJob received = (GameMatchJob) message;
    assertThat(received.getCompetitionSlug()).isEqualTo(competition.getSlug());
    assertThat(received.getLadder()).isEqualTo(ladder);
    assertThat(received.getUuid()).isEqualTo(match.getUuid().toString());
  }

  @Test
  void getPaginatedMatchesFiltersByOpponentAndTeamWin() {
    Competition competition =
        testDataFactory.createCompetition("comp-filters", "Competition Filters", true, 2);
    String ladder = "ranked";
    testDataFactory.createLadder(competition, ladder);

    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamC = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    User user = testDataFactory.createUser();

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);
    Submission submissionC = createSubmission(teamC);

    GameMatch teamABWin =
        createCompletedMatch(
            user, teamA, teamB, submissionA, submissionB, ladder, MatchStatus.team_a_win);
    GameMatch teamABLose =
        createCompletedMatch(
            user, teamA, teamB, submissionA, submissionB, ladder, MatchStatus.team_b_win);

    createCompletedMatch(
        user, teamA, teamC, submissionA, submissionC, ladder, MatchStatus.team_a_win);

    PageRequest page = PageRequest.of(0, 10);

    var anyAgainstB =
        gameMatchService.getPaginatedMatches(
            competition,
            ladder,
            teamA.getUuid().toString(),
            teamB.getName().toUpperCase(Locale.ROOT),
            "Any",
            null,
            null,
            null,
            null,
            null,
            page);
    assertThat(anyAgainstB.getContent())
        .extracting(GameMatch::getUuid)
        .containsExactlyInAnyOrder(teamABWin.getUuid(), teamABLose.getUuid());

    var winsAgainstB =
        gameMatchService.getPaginatedMatches(
            competition,
            ladder,
            teamA.getUuid().toString(),
            teamB.getName().toUpperCase(Locale.ROOT),
            "Win",
            null,
            null,
            null,
            null,
            null,
            page);
    assertThat(winsAgainstB.getContent())
        .extracting(GameMatch::getUuid)
        .containsExactly(teamABWin.getUuid());

    var lossesAgainstB =
        gameMatchService.getPaginatedMatches(
            competition,
            ladder,
            teamA.getUuid().toString(),
            teamB.getName().toUpperCase(Locale.ROOT),
            "Lose",
            null,
            null,
            null,
            null,
            null,
            page);
    assertThat(lossesAgainstB.getContent())
        .extracting(GameMatch::getUuid)
        .containsExactly(teamABLose.getUuid());

    var nullTeamWinAgainstB =
        gameMatchService.getPaginatedMatches(
            competition,
            ladder,
            teamA.getUuid().toString(),
            teamB.getName(),
            null,
            null,
            null,
            null,
            null,
            null,
            page);
    assertThat(nullTeamWinAgainstB.getContent())
        .extracting(GameMatch::getUuid)
        .containsExactlyInAnyOrder(teamABWin.getUuid(), teamABLose.getUuid());

    var unknownOpponentTeam =
        gameMatchService.getPaginatedMatches(
            competition,
            ladder,
            teamA.getUuid().toString(),
            "not-a-real-team-name",
            "Any",
            null,
            null,
            null,
            null,
            null,
            page);
    assertThat(unknownOpponentTeam.getContent()).isEmpty();
  }

  @Test
  void getPaginatedMatchesTeamWinRequiresTeamUuid() {
    Competition competition =
        testDataFactory.createCompetition("comp-team-win", "Competition", true, 2);
    String ladder = "ranked";
    testDataFactory.createLadder(competition, ladder);

    assertThatThrownBy(
            () ->
                gameMatchService.getPaginatedMatches(
                    competition,
                    ladder,
                    null,
                    null,
                    "Win",
                    null,
                    null,
                    null,
                    null,
                    null,
                    PageRequest.of(0, 10)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("teamUuid is required when teamWin is Win or Lose");
  }

  private GameMatch createCompletedMatch(
      User creatingUser,
      Team teamA,
      Team teamB,
      Submission submissionA,
      Submission submissionB,
      String ladder,
      MatchStatus status) {
    GameMatch match =
        gameMatchService.createMatch(
            creatingUser,
            teamA,
            teamB,
            submissionA,
            submissionB,
            ladder,
            MatchReason.matchmaking,
            null,
            null);
    match.setStatus(status);
    return gameMatchRepository.save(match);
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
