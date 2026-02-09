package org.bytefight.webserver.gamematch;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchUpdate;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.rabbitmq.infra.RabbitMQConfiguration;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "is-prod-env=true")
class GameMatchQueueIT extends FullStackIntegrationTestBase {
    @Autowired
    private GameMatchService gameMatchService;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange gameMatchExchange;

    @Autowired
    private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

    @Autowired
    private GameMatchRepository gameMatchRepository;

    @Test
    void scheduleMatchAndUpdateStatusThroughQueue() throws Exception {
        String competitionSlug = "comp";
        Competition competition = testDataFactory.createCompetition(competitionSlug, "Competition", true, 2);
        String ladder = "ladder1";
        testDataFactory.createLadder(competition, ladder);
        Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
        Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
        User user = testDataFactory.createUser();

        Submission submissionA = createSubmission(teamA);
        Submission submissionB = createSubmission(teamB);

        String routingKey = "competition." + competitionSlug + "." + ladder;

        Queue scheduleQueue = QueueBuilder.nonDurable("test.match.queue").autoDelete().build();
        Queue updateQueue = QueueBuilder.durable(RabbitMQConfiguration.GAME_MATCH_UPDATES).build();
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(gameMatchExchange);
        admin.declareQueue(scheduleQueue);
        admin.declareQueue(updateQueue);
        Binding binding = BindingBuilder.bind(scheduleQueue).to(gameMatchExchange).with(routingKey);
        admin.declareBinding(binding);

        GameMatch match = gameMatchService.createMatch(
                user,
                teamA,
                teamB,
                submissionA,
                submissionB,
                ladder,
                MatchReason.matchmaking,
                null
        );

        gameMatchService.scheduleMatch(match);

        Object scheduledMessage = rabbitTemplate.receiveAndConvert(scheduleQueue.getName(), 2000);
        assertThat(scheduledMessage).isInstanceOf(GameMatchJob.class);

        UUID matchUuid = (UUID) ReflectionTestUtils.getField(match, "uuid");
        GameMatchUpdate update = createUpdate(matchUuid.toString(), true);
        rabbitTemplate.convertAndSend(RabbitMQConfiguration.GAME_MATCH_UPDATES, update);

        assertThat(waitForStatus(matchUuid, MatchStatus.in_progress)).isTrue();
    }

    private boolean waitForStatus(UUID matchUuid, MatchStatus status) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            GameMatch refreshed = gameMatchRepository.findByUuid(matchUuid).orElse(null);
            if (refreshed != null) {
                if (refreshed.getStatus() == status) {
                    return true;
                }
            }
            Thread.sleep(200);
        }
        return false;
    }

    private GameMatchUpdate createUpdate(String uuid, boolean started) {
        try {
            var constructor = GameMatchUpdate.class.getDeclaredConstructor(String.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(uuid, started);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create GameMatchUpdate", ex);
        }
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
