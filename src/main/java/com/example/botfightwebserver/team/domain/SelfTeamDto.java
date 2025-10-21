package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.submission.SubmissionDTO;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Value
@Builder
public class SelfTeamDto {
    @NotNull Long id;
    @NotNull String name;
    @NotNull Double glicko;
    @NotNull Integer matchesPlayed;
    @NotNull Integer numberWins;
    @NotNull Integer numberLosses;
    @NotNull Integer numberDraws;
    @NotNull String quote;
    @NotNull Integer numberOfPlayers;
    SubmissionDTO currentSubmissionDTO;
    String teamCode;
    @NotNull LocalDateTime creationDateTime;
    @NotNull LocalDateTime lastModifiedDate;


    public static SelfTeamDto from(Team team) {
        return SelfTeamDto.builder()
                .id(team.getId())
                .name(team.getName())
                .creationDateTime(team.getCreationDateTime())
                .lastModifiedDate(team.getLastModifiedDate())
                .glicko(team.getGlicko())
                .matchesPlayed(team.getMatchesPlayed())
                .numberWins(team.getNumberWins())
                .numberLosses(team.getNumberLosses())
                .numberDraws(team.getNumberDraws())
                .quote(team.getQuote())
                .currentSubmissionDTO(
                        team.getCurrentSubmission() != null ? SubmissionDTO.fromEntity(team.getCurrentSubmission()) : null)
                .teamCode(team.getTeamCode())
                .numberOfPlayers(team.getNumberPlayers())
                .build();
    }
}
