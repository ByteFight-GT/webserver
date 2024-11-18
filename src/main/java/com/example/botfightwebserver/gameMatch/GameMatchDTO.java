package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.player.PlayerDTO;
import com.example.botfightwebserver.submission.Submission;
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
    private PlayerDTO playerOne;
    private PlayerDTO playerTwo;
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
            .playerOne(PlayerDTO.fromEntity(gameMatch.getPlayerOne()))
            .playerTwo(PlayerDTO.fromEntity(gameMatch.getPlayerTwo()))
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