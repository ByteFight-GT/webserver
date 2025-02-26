package com.example.botfightwebserver.team;

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
public class TeamDTO {
    private Long id;
    private String name;
    private LocalDateTime creationDateTime;
    private LocalDateTime lastModifiedDate;
    private Double glicko;
    private Double phi;
    private Double sigma;
    private Integer matchesPlayed;
    private Integer numberWins;
    private Integer numberLosses;
    private Integer numberDraws;
    private String quote;
    private Integer numberOfPlayers;
    private SubmissionDTO currentSubmissionDTO;
    private String teamCode;

    public static TeamDTO fromEntity(Team team) {
        return TeamDTO.builder()
            .id(team.getId())
            .name(team.getName())
            .creationDateTime(team.getCreationDateTime())
            .lastModifiedDate(team.getLastModifiedDate())
            .glicko(team.getGlicko())
                .phi(team.getPhi())
                .sigma(team.getSigma())
            .matchesPlayed(team.getMatchesPlayed())
            .numberWins(team.getNumberWins())
            .numberLosses(team.getNumberLosses())
            .numberDraws(team.getNumberDraws())
            .quote(team.getQuote())
            .currentSubmissionDTO(
                team.getCurrentSubmission() != null? SubmissionDTO.fromEntity(team.getCurrentSubmission()): null)
            .teamCode(team.getTeamCode())
            .numberOfPlayers(team.getNumberPlayers())
            .build();
    }
}
