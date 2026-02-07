package org.bytefight.webserver.team.domain.dto;

import lombok.Value;
import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamType;

@Value
public class AdminTeamDto {
    Long id;
    String uuid;
    Long competitionId;
    String name;
    String quote;
    String joinCode;
    boolean displayMembers;
    TeamType type;
    boolean isDeleted;
    TimestampsDto timestamps;

    public static AdminTeamDto from(Team team) {
        return new AdminTeamDto(
                team.getId(),
                team.getUuid().toString(),
                team.getCompetition().getId(),
                team.getName(),
                team.getQuote(),
                team.getJoinCode(),
                team.isDisplayMembers(),
                team.getType(),
                team.isDeleted(),
                TimestampsDto.from(team)
        );
    }
}
