package com.example.botfightwebserver.submission;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionDTOTest {

    @Test
    void fromEntity_ShouldCorrectlyMapAllFields() {
        // Arrange
        Long id = 1L;
        Long teamId = 2L;
        SUBMISSION_VALIDITY validity = SUBMISSION_VALIDITY.VALID;
        LocalDateTime createdAt = LocalDateTime.now();

        Submission submission = new Submission();
        submission.setId(id);
        submission.setTeamId(teamId);
        submission.setSubmissionValidity(validity);
        submission.setCreatedAt(createdAt);

        // Act
        SubmissionDTO dto = SubmissionDTO.fromEntity(submission);

        // Assert
        assertNotNull(dto, "DTO should not be null");
        assertEquals(id, dto.getId(), "ID should match");
        assertEquals(teamId, dto.getTeamId(), "Team ID should match");
        assertEquals(validity, dto.getValidity(), "Validity should match");
        assertEquals(createdAt, dto.getCreatedAt(), "Created at should match");
    }

    @Test
    void fromEntity_ShouldHandleNullSubmission() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> SubmissionDTO.fromEntity(null),
                "Should throw NullPointerException when submission is null"
        );
    }

    @Test
    void recordMethods_ShouldWorkCorrectly() {
        // Arrange
        Long id = 1L;
        Long teamId = 2L;
        SUBMISSION_VALIDITY validity = SUBMISSION_VALIDITY.VALID;
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        SubmissionDTO dto1 = new SubmissionDTO(id, teamId, validity, createdAt, "same");
        SubmissionDTO dto2 = new SubmissionDTO(id, teamId, validity, createdAt, "same");
        SubmissionDTO differentDto = new SubmissionDTO(3L, teamId, validity, createdAt, "different");

        // Assert
        // Test equals and hashCode
        assertEquals(dto1, dto2, "DTOs with same values should be equal");
        assertEquals(dto1.hashCode(), dto2.hashCode(), "Hash codes should be equal for equal DTOs");
        assertNotEquals(dto1, differentDto, "DTOs with different values should not be equal");

        // Test getters
        assertEquals(id, dto1.getId(), "id getter should work");
        assertEquals(teamId, dto1.getTeamId(), "teamId getter should work");
        assertEquals(validity, dto1.getValidity(), "validity getter should work");
        assertEquals(createdAt, dto1.getCreatedAt(), "createdAt getter should work");

        // Test toString
        String toString = dto1.toString();
        assertTrue(toString.contains(id.toString()), "toString should contain id");
        assertTrue(toString.contains(teamId.toString()), "toString should contain teamId");
        assertTrue(toString.contains(validity.toString()), "toString should contain validity");
        assertTrue(toString.contains(createdAt.toString()), "toString should contain createdAt");
    }

    @Test
    void constructor_ShouldAcceptNullFields() {
        // Act
        SubmissionDTO dto = new SubmissionDTO(null, null, null, null, null);

        // Assert
        assertNull(dto.getId(), "ID should be null");
        assertNull(dto.getTeamId(), "Team ID should be null");
        assertNull(dto.getValidity(), "Validity should be null");
        assertNull(dto.getCreatedAt(), "Created at should be null");
        assertNull(dto.getName(), "Name should be null");
    }
}