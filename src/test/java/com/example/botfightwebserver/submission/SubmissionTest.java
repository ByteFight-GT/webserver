package com.example.botfightwebserver.submission;

import com.example.botfightwebserver.PersistentTestBase;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class SubmissionTest extends PersistentTestBase {

    private Submission testSubmission;
    private Clock fixedClock;

    private final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    @BeforeEach
    void setup() {
        fixedClock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        Submission.setClock(fixedClock);
        testSubmission = Submission.builder()
                .storagePath("/fake/storage/path")
                .teamId(1L)
                .submissionValidity(SUBMISSION_VALIDITY.VALID)
                .source(STORAGE_SOURCE.LOCAL)
                .build();
    }

    @Test
    void testBuilderWithAllFields() {
        Submission submission = Submission.builder()
                .id(1L)
                .storagePath("/fake/storage/path")
                .teamId(2L)
                .submissionValidity(SUBMISSION_VALIDITY.VALID)
                .source(STORAGE_SOURCE.GCP)
                .createdAt(NOW)
                .validateAt(NOW.plusHours(1))
                .build();

        assertEquals(1L, submission.getId());
        assertEquals("/fake/storage/path", submission.getStoragePath());
        assertEquals(2L, submission.getTeamId());
        assertEquals(SUBMISSION_VALIDITY.VALID, submission.getSubmissionValidity());
        assertEquals(STORAGE_SOURCE.GCP, submission.getSource());
        assertEquals(NOW, submission.getCreatedAt());
        assertEquals(NOW.plusHours(1), submission.getValidateAt());
    }

    @Test
    void testDefaultValuesOnPersist() {
        persistEntity(testSubmission);
        assertNotNull(testSubmission.getCreatedAt());
        assertNull(testSubmission.getValidateAt());
    }

    @Test
    void testPrePersist() {
        persistEntity(testSubmission);
        assertEquals(NOW, testSubmission.getCreatedAt());
    }

    @Test
    void testSetSubmissionValidityUpdatesValidateAt() {
        testSubmission.setSubmissionValidity(SUBMISSION_VALIDITY.VALID);
        assertEquals(NOW, testSubmission.getValidateAt());
    }


    @Test
    void testClockUpdate() {
        LocalDateTime dayAfter = NOW.plusDays(1);
        Clock newClock = Clock.fixed(dayAfter.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        Submission.setClock(newClock);

        testSubmission.setSubmissionValidity(SUBMISSION_VALIDITY.VALID);
        assertEquals(dayAfter, testSubmission.getValidateAt());
    }
}
