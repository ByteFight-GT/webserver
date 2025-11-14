package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.submission.domain.SubmissionDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

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
    @NotNull Integer rank;
    @NotNull TeamType type;
    List<String> memberNames;
    SubmissionDTO currentSubmissionDTO;

    public static PublicTeamDto from(Team team, int rank, List<String> memberNames) {
        return PublicTeamDto.builder().uuid(team.getUuid().toString())
                .name(team.getName())
                .creationDateTime(team.getCreationDateTime())
                .glicko(team.getGlicko())
                .matchesPlayed(team.getMatchesPlayed())
                .numberWins(team.getNumberWins())
                .numberLosses(team.getNumberLosses())
                .numberDraws(team.getNumberDraws())
                .quote(team.getQuote())
                .currentSubmissionDTO(team.getCurrentSubmission() != null ? SubmissionDTO.from(team.getCurrentSubmission()) : null)
                .numberOfPlayers(team.getNumberPlayers())
                .rank(rank)
                .memberNames(memberNames)
                .build();
    }
}
