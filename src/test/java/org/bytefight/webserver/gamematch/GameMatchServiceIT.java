package org.bytefight.webserver.gamematch;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.rabbitmq.infra.RabbitMQConfiguration;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameMatchServiceIT extends FullStackIntegrationTestBase {
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

        GameMatch match = gameMatchService.createMatch(
                user,
                teamA,
                teamB,
                submissionA,
                submissionB,
                ladder,
                MatchReason.ladder,
                null
        );
        match.setMatchSettings(Map.of("map", "arena_01"));

        gameMatchService.scheduleMatch(match);

        Object message = rabbitTemplate.receiveAndConvert(queue.getName(), 2000);
        assertThat(message).isInstanceOf(GameMatchJob.class);

        GameMatchJob received = (GameMatchJob) message;
        assertThat(received.getCompetitionSlug()).isEqualTo(competition.getSlug());
        assertThat(received.getLadder()).isEqualTo(ladder);
        assertThat(received.getUuid()).isEqualTo(match.getUuid().toString());
    }

    private Submission createSubmission(Team team) {
        FileRecord record = FileRecord.builder()
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
