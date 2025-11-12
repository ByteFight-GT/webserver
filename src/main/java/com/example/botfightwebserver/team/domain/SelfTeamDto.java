package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.submission.domain.SubmissionDTO;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Value
@Builder
public class SelfTeamDto {
    @NotNull String uuid;
    @NotNull String name;
    @NotNull String quote;
    @NotNull Boolean displayMembers;
    @NotNull Double glicko;
    @NotNull Integer matchesPlayed;
    @NotNull Integer numberWins;
    @NotNull Integer numberLosses;
    @NotNull Integer numberDraws;
    @NotNull Integer numberOfPlayers;
    @NotNull Integer rank;
    @NotNull TeamType type;
    SubmissionDTO currentSubmissionDTO;
    String teamCode;
    @NotNull LocalDateTime creationDateTime;
    @NotNull LocalDateTime lastModifiedDate;
    @NotNull Boolean isDeleted;


    public static SelfTeamDto from(Team team, int rank) {
        return SelfTeamDto.builder()
                .uuid(team.getUuid().toString())
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
                        team.getCurrentSubmission() != null ? SubmissionDTO.from(team.getCurrentSubmission()) : null)
                .teamCode(team.getTeamCode())
                .numberOfPlayers(team.getNumberPlayers())
                .rank(rank)
                .type(team.getType())
                .displayMembers(team.isDisplayMembers())
                .isDeleted(team.isDeleted())
                .build();
    }
}
