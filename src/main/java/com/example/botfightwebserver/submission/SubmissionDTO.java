package com.example.botfightwebserver.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDTO {
    private Long id;
    private Long teamId;
    private SUBMISSION_VALIDITY validity;
    private LocalDateTime createdAt;
    private String name;
    private String storagePath;

    public static SubmissionDTO fromEntity(Submission submission) {
        return new SubmissionDTO(
            submission.getId(),
            submission.getTeamId(),
            submission.getSubmissionValidity(),
            submission.getCreatedAt(),
            submission.getName(),
            submission.getStoragePath()
        );
    }
}