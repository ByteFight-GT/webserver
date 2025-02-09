package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.team.TeamDTO;
import com.example.botfightwebserver.submission.SubmissionDTO;
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
    private Long id;
    private String teamOneName;
    private String teamTwoName;
    private Long teamOneId;
    private Long teamTwoId;
    private String submissionOneName;
    private String submissionTwoName;
    private MATCH_STATUS status;
    private MATCH_REASON reason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Integer timesQueued;

    // Convert from Entity to DTO
    public static GameMatchDTO fromEntity(GameMatch gameMatch) {
        return GameMatchDTO.builder()
            .id(gameMatch.getId())
            .teamOneName(gameMatch.getTeamOne().getName())
            .teamTwoName(gameMatch.getTeamTwo().getName())
            .teamOneId(gameMatch.getTeamOne().getId())
            .teamTwoId(gameMatch.getTeamTwo().getId())
            .submissionOneName(gameMatch.getSubmissionOne().getName())
            .submissionTwoName(gameMatch.getSubmissionTwo().getName())
            .status(gameMatch.getStatus())
            .reason(gameMatch.getReason())
            .createdAt(gameMatch.getCreatedAt())
            .processedAt(gameMatch.getProcessedAt())
            .timesQueued(gameMatch.getTimesQueued())
            .build();
    }
}