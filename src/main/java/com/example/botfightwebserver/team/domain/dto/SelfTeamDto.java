package com.example.botfightwebserver.team.domain.dto;

import com.example.botfightwebserver.common.domain.dto.TimestampsDto;
import com.example.botfightwebserver.competition.domain.dto.CompetitionDto;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.domain.PublicPlayerDto;
import com.example.botfightwebserver.submission.domain.SubmissionDTO;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.domain.TeamType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
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
    SubmissionDTO currentSubmissionDTO;
    @NotNull TeamType type;
    @NotNull List<PublicPlayerDto> members;
    @NotNull TimestampsDto timestampsDto;

    public static SelfTeamDto from(Team team, List<Player> members) {
        return SelfTeamDto.builder()
                .competition(CompetitionDto.from(team.getCompetition()))
                .uuid(team.getUuid().toString())
                .name(team.getName())
                .quote(team.getQuote())
                .joinCode(team.getJoinCode())
                .displayMembers(team.isDisplayMembers())
                .currentSubmissionDTO(
                        team.getCurrentSubmission() != null ? SubmissionDTO.from(team.getCurrentSubmission()) : null)
                .type(team.getType())
                .members(members.stream().map(PublicPlayerDto::from).toList())
                .timestampsDto(TimestampsDto.from(team))
                .build();
    }
}
