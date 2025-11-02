package com.example.botfightwebserver.gameMatch.domain;

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
public class GameMatchDTO {
    private String uuid;
    @NotNull private String teamOneName;
    @NotNull private String teamTwoName;
    @NotNull private String teamOneUuid;
    @NotNull private String teamTwoUuid;
    private String submissionOneName;
    private String submissionTwoName;
    private MATCH_STATUS status;
    private MATCH_REASON reason;
    @NotNull private LocalDateTime createdAt;
    @NotNull private LocalDateTime processedAt;
    private Integer timesQueued;
    private String map;

    // Convert from Entity to DTO
    public static GameMatchDTO fromEntity(GameMatch gameMatch) {
        return GameMatchDTO.builder()
            .uuid(gameMatch.getUuid().toString())
            .teamOneName(gameMatch.getTeamOne().getName())
            .teamTwoName(gameMatch.getTeamTwo().getName())
            .teamOneUuid(gameMatch.getTeamOne().getUuid().toString())
            .teamTwoUuid(gameMatch.getTeamTwo().getUuid().toString())
            .submissionOneName(gameMatch.getSubmissionOne().getName())
            .submissionTwoName(gameMatch.getSubmissionTwo().getName())
            .status(gameMatch.getStatus())
            .reason(gameMatch.getReason())
            .createdAt(gameMatch.getCreatedAt())
            .processedAt(gameMatch.getProcessedAt())
            .timesQueued(gameMatch.getTimesQueued())
            .map(gameMatch.getMap())
            .build();
    }
}