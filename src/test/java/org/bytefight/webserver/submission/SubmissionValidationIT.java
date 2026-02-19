package org.bytefight.webserver.submission;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchResult;
import org.bytefight.webserver.rabbitmq.infra.RabbitMQConfiguration;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "is-prod-env=true")
class SubmissionValidationIT extends FullStackIntegrationTestBase {
    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private GameMatchService gameMatchService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void validationResultMarksSubmissionValidAndAutoSetsCurrentSubmission() throws Exception {
        Competition competition = testDataFactory.createCompetition("comp-sub-valid", "Competition", true, 2);
        testDataFactory.createLadder(competition, DefaultLadders.VALIDATION);
        Team team = testDataFactory.createTeam(competition, UUID.randomUUID(), false);

        Submission submission = createSubmission(team, SubmissionValidity.not_evaluated_autoset);
        GameMatch match = createValidationMatch(team, submission);

        GameMatchResult result = new GameMatchResult(getMatchUuid(match), MatchStatus.submission_valid);
        rabbitTemplate.convertAndSend(RabbitMQConfiguration.GAME_MATCH_RESULTS, result);

        assertThat(waitForSubmissionValidity(getSubmissionUuid(submission), SubmissionValidity.valid)).isTrue();
    }

    @Test
    void validationResultMarksSubmissionInvalidWithoutAutoSet() throws Exception {
        Competition competition = testDataFactory.createCompetition("comp-sub-invalid", "Competition", true, 2);
        testDataFactory.createLadder(competition, DefaultLadders.VALIDATION);
        Team team = testDataFactory.createTeam(competition, UUID.randomUUID(), false);

        Submission submission = createSubmission(team, SubmissionValidity.not_evaluated);
        GameMatch match = createValidationMatch(team, submission);

        GameMatchResult result = new GameMatchResult(getMatchUuid(match), MatchStatus.submission_invalid);
        rabbitTemplate.convertAndSend(RabbitMQConfiguration.GAME_MATCH_RESULTS, result);

        assertThat(waitForSubmissionValidity(getSubmissionUuid(submission), SubmissionValidity.invalid)).isTrue();
    }

    private Submission createSubmission(Team team, SubmissionValidity validity) {
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
        ReflectionTestUtils.setField(submission, "validity", validity);
        return submissionRepository.save(submission);
    }

    private GameMatch createValidationMatch(Team team, Submission submission) {
        return gameMatchService.createMatch(
                null,
                team,
                team,
                submission,
                submission,
                DefaultLadders.VALIDATION,
                MatchReason.validation,
                null,
                null
        );
    }

    private boolean waitForSubmissionValidity(UUID submissionUuid, SubmissionValidity expected) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            Submission refreshed = submissionRepository.findSubmissionByUuid(submissionUuid).orElse(null);
            if (refreshed != null) {
                SubmissionValidity validity = (SubmissionValidity) ReflectionTestUtils.getField(refreshed, "validity");
                if (validity == expected) {
                    return true;
                }
            }
            Thread.sleep(200);
        }
        return false;
    }

    private UUID getSubmissionUuid(Submission submission) {
        return (UUID) ReflectionTestUtils.getField(submission, "uuid");
    }

    private String getMatchUuid(GameMatch match) {
        UUID uuid = (UUID) ReflectionTestUtils.getField(match, "uuid");
        return uuid.toString();
    }

}
