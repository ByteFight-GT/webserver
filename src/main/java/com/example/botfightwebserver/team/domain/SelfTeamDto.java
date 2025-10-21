package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.submission.SubmissionDTO;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Value
@SuperBuilder
public class SelfTeamDto extends PublicTeamDto {
    @NotNull String teamCode;
    @NotNull LocalDateTime lastModifiedDate;

    public static SelfTeamDto from(Team team) {
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
                        team.getCurrentSubmission() != null ? SubmissionDTO.fromEntity(team.getCurrentSubmission()) : null)
                .teamCode(team.getTeamCode())
                .numberOfPlayers(team.getNumberPlayers())
                .build();
    }
}
