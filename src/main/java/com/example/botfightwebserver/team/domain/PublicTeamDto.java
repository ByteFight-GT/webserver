package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.submission.domain.SubmissionDTO;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Value
@Builder
public class PublicTeamDto {
    @NotNull String uuid;
    @NotNull String name;
    @NotNull LocalDateTime creationDateTime;
    @NotNull Double glicko;
    @NotNull Integer matchesPlayed;
    @NotNull Integer numberWins;
    @NotNull Integer numberLosses;
    @NotNull Integer numberDraws;
    @NotNull String quote;
    @NotNull Integer numberOfPlayers;
    SubmissionDTO currentSubmissionDTO;

    public static PublicTeamDto from(Team team) {
        return PublicTeamDto.builder()
                .uuid(team.getUuid().toString())
                .name(team.getName())
                .creationDateTime(team.getCreationDateTime())
                .glicko(team.getGlicko())
                .matchesPlayed(team.getMatchesPlayed())
                .numberWins(team.getNumberWins())
                .numberLosses(team.getNumberLosses())
                .numberDraws(team.getNumberDraws())
                .quote(team.getQuote())
                .currentSubmissionDTO(team.getCurrentSubmission() != null ? SubmissionDTO.fromEntity(team.getCurrentSubmission()) : null)
                .numberOfPlayers(team.getNumberPlayers())
                .build();
    }
}
