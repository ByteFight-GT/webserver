package org.bytefight.webserver.matchmaking;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.matchmaking.application.MatchmakingService;
import org.bytefight.webserver.rabbitmq.infra.RabbitMQConfiguration;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MatchmakingServiceIT extends FullStackIntegrationTestBase {
    @Autowired
    private MatchmakingService matchmakingService;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private GameMatchRepository gameMatchRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange gameMatchExchange;

    @Autowired
    private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

    @Test
    void createAndScheduleEventQueuesMatches() {
        String competitionSlug = "comp-mm";
        Competition competition = testDataFactory.createCompetition(competitionSlug, "Competition", true, 2);
        String ladder = "main";
        testDataFactory.createLadder(competition, ladder);

        Team teamA = createTeamWithSubmission(competition);
        Team teamB = createTeamWithSubmission(competition);
        Team teamC = createTeamWithSubmission(competition);
        Team teamD = createTeamWithSubmission(competition);

        String routingKey = "competition." + competitionSlug + "." + ladder;
        Queue scheduleQueue = QueueBuilder.nonDurable("test.matchmaking.queue").build();
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(gameMatchExchange);
        admin.declareQueue(scheduleQueue);
        Binding binding = BindingBuilder.bind(scheduleQueue).to(gameMatchExchange).with(routingKey);
        admin.declareBinding(binding);

        matchmakingService.createAndScheduleEvent(competition, ladder);

        int queued = 0;
        Object message;
        while ((message = rabbitTemplate.receiveAndConvert(scheduleQueue.getName(), 1000)) != null) {
            assertThat(message).isInstanceOf(GameMatchJob.class);
            queued++;
        }

        assertThat(queued).isEqualTo(6);

        List<GameMatch> matches = gameMatchRepository.findAll().stream()
                .filter(match -> competition.equals(ReflectionTestUtils.getField(match, "competition")))
                .filter(match -> ladder.equals(ReflectionTestUtils.getField(match, "ladder")))
                .toList();

        assertThat(matches).hasSize(6);
        assertThat(matches).allSatisfy(match -> {
            assertThat(ReflectionTestUtils.getField(match, "reason")).isEqualTo(MatchReason.matchmaking);
            assertThat(ReflectionTestUtils.getField(match, "status")).isEqualTo(MatchStatus.waiting);
        });
    }

    @Test
    void createAndScheduleEventWithSixTeamsQueuesTwelveMatches() {
        String competitionSlug = "comp-mm-large";
        Competition competition = testDataFactory.createCompetition(competitionSlug, "Competition", true, 2);
        String ladder = "main";
        testDataFactory.createLadder(competition, ladder);

        for (int i = 0; i < 6; i++) {
            createTeamWithSubmission(competition);
        }

        String routingKey = "competition." + competitionSlug + "." + ladder;
        Queue scheduleQueue = QueueBuilder.nonDurable("test.matchmaking.queue.large").build();
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(gameMatchExchange);
        admin.declareQueue(scheduleQueue);
        Binding binding = BindingBuilder.bind(scheduleQueue).to(gameMatchExchange).with(routingKey);
        admin.declareBinding(binding);

        matchmakingService.createAndScheduleEvent(competition, ladder);

        int queued = 0;
        Object message;
        while ((message = rabbitTemplate.receiveAndConvert(scheduleQueue.getName(), 1000)) != null) {
            assertThat(message).isInstanceOf(GameMatchJob.class);
            queued++;
        }

        assertThat(queued).isEqualTo(12);

        List<GameMatch> matches = gameMatchRepository.findAll().stream()
                .filter(match -> competition.equals(ReflectionTestUtils.getField(match, "competition")))
                .filter(match -> ladder.equals(ReflectionTestUtils.getField(match, "ladder")))
                .toList();

        assertThat(matches).hasSize(12);
        assertThat(matches).allSatisfy(match -> {
            assertThat(ReflectionTestUtils.getField(match, "reason")).isEqualTo(MatchReason.matchmaking);
            assertThat(ReflectionTestUtils.getField(match, "status")).isEqualTo(MatchStatus.waiting);
        });
    }

    private Team createTeamWithSubmission(Competition competition) {
        Team team = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
        Submission submission = createSubmission(team);
        ReflectionTestUtils.setField(team, "currentSubmission", submission);
        return teamRepository.save(team);
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
