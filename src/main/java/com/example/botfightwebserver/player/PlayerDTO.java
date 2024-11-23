package com.example.botfightwebserver.player;

import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.submission.SubmissionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime creationDateTime;
    private LocalDateTime lastModifiedDate;
    private Double glicko;
    private Double phi;
    private Double sigma;
    private Integer matchesPlayed;
    private Integer numberWins;
    private Integer numberLosses;
    private Integer numberDraws;
    private SubmissionDTO currentSubmissionDTO;

    public static PlayerDTO fromEntity(Player player) {
        return PlayerDTO.builder()
            .id(player.getId())
            .name(player.getName())
            .email(player.getEmail())
            .creationDateTime(player.getCreationDateTime())
            .lastModifiedDate(player.getLastModifiedDate())
            .glicko(player.getGlicko())
                .phi(player.getPhi())
                .sigma(player.getSigma())
            .matchesPlayed(player.getMatchesPlayed())
            .numberWins(player.getNumberWins())
            .numberLosses(player.getNumberLosses())
            .numberDraws(player.getNumberDraws())
            .currentSubmissionDTO(player.getCurrentSubmission() != null? SubmissionDTO.fromEntity(player.getCurrentSubmission()): null)
            .build();
    }
}
