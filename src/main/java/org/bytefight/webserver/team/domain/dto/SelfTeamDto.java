package org.bytefight.webserver.team.domain.dto;

import org.bytefight.webserver.common.domain.dto.DeletionDto;
import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.competition.domain.dto.CompetitionDto;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.domain.PublicPlayerDto;
import org.bytefight.webserver.submission.domain.dto.SubmissionDto;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Value
@Builder
public class SelfTeamDto {
    @NotNull CompetitionDto competition;
    @NotNull String uuid;
    @NotNull String name;
    @NotNull String quote;
    String joinCode;
    @NotNull Boolean displayMembers;
    SubmissionDto currentSubmissionDTO;
    @NotNull TeamType type;
    @NotNull List<PublicPlayerDto> members;
    @NotNull TimestampsDto timestampsDto;
    @NotNull DeletionDto deletionDto;

    public static SelfTeamDto from(Team team, List<Player> members) {
        return SelfTeamDto.builder()
                .competition(CompetitionDto.from(team.getCompetition()))
                .uuid(team.getUuid().toString())
                .name(team.getName())
                .quote(team.getQuote())
                .joinCode(team.getJoinCode())
                .displayMembers(team.isDisplayMembers())
                .currentSubmissionDTO(
                        team.getCurrentSubmission() != null ? SubmissionDto.from(team.getCurrentSubmission()) : null)
                .type(team.getType())
                .members(members.stream().map(PublicPlayerDto::from).toList())
                .timestampsDto(TimestampsDto.from(team))
                .deletionDto(DeletionDto.from(team))
                .build();
    }
}
