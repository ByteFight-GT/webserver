package com.example.botfightwebserver.submission;

import java.time.LocalDateTime;

public record SubmissionDTO(Long id, Long teamId, SUBMISSION_VALIDITY validity, LocalDateTime createdAt) {
    public static SubmissionDTO fromEntity(Submission submission) {
        return new SubmissionDTO(
            submission.getId(),
            submission.getTeamId(),
            submission.getSubmissionValidity(),
            submission.getCreatedAt()
        );
    }
}
