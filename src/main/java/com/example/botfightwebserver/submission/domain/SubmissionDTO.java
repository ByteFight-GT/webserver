package com.example.botfightwebserver.submission.domain;

import jakarta.validation.constraints.NotNull;
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
    @NotNull private String uuid;
    @NotNull private String teamUuid;
    @NotNull private SubmissionValidity validity;
    @NotNull private LocalDateTime createdAt;
    @NotNull private String name;
    @NotNull private Boolean isAutoSet;

    public static SubmissionDTO from(Submission submission) {
        return new SubmissionDTO(
            submission.getUuid().toString(),
            submission.getTeam().getUuid().toString(),
            submission.getSubmissionValidity(),
            submission.getCreatedAt(),
            submission.getName(),
            submission.getIsAutoSet()
        );
    }
}