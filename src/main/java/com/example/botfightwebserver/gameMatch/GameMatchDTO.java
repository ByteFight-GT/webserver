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
    private TeamDTO teamOne;
    private TeamDTO teamTwo;
    private SubmissionDTO submissionOne;
    private SubmissionDTO submissionTwo;
    private MATCH_STATUS status;
    private MATCH_REASON reason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Integer timesQueued;

    // Convert from Entity to DTO
    public static GameMatchDTO fromEntity(GameMatch gameMatch) {
        return GameMatchDTO.builder()
            .id(gameMatch.getId())
            .teamOne(TeamDTO.fromEntity(gameMatch.getTeamOne()))
            .teamTwo(TeamDTO.fromEntity(gameMatch.getTeamTwo()))
            .submissionOne(SubmissionDTO.fromEntity(gameMatch.getSubmissionOne()))
            .submissionTwo(SubmissionDTO.fromEntity(gameMatch.getSubmissionTwo()))
            .status(gameMatch.getStatus())
            .reason(gameMatch.getReason())
            .createdAt(gameMatch.getCreatedAt())
            .processedAt(gameMatch.getProcessedAt())
            .timesQueued(gameMatch.getTimesQueued())
            .build();
    }
}