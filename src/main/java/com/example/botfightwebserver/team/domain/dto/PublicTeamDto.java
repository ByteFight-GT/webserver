package com.example.botfightwebserver.team.domain.dto;

import com.example.botfightwebserver.common.domain.dto.TimestampsDto;
import com.example.botfightwebserver.competition.domain.dto.CompetitionDto;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.domain.PublicPlayerDto;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.domain.TeamType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class PublicTeamDto {
    @NotNull CompetitionDto competition;
    @NotNull String uuid;
    @NotNull String name;
    @NotNull String quote;
    @NotNull Boolean displayMembers;
    @NotNull TeamType type;
    @NotNull TimestampsDto timestampsDto;
    List<PublicPlayerDto> members;

    public static PublicTeamDto from(Team team, List<Player> members) {
        return PublicTeamDto.builder()
                .competition(CompetitionDto.from(team.getCompetition()))
                .uuid(team.getUuid().toString())
                .name(team.getName())
                .quote(team.getQuote())
                .displayMembers(team.isDisplayMembers())
                .type(team.getType())
                .members(members != null ? members.stream().map(PublicPlayerDto::from).toList() : null)
                .timestampsDto(TimestampsDto.from(team))
                .build();
    }
}
