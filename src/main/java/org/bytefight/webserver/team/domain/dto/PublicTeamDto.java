package org.bytefight.webserver.team.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

import org.bytefight.webserver.common.domain.dto.DeletionDto;
import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.competition.domain.dto.CompetitionDto;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.domain.dto.PublicPlayerDto;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamType;

@Value
@Builder
public class PublicTeamDto {
  @NotNull CompetitionDto competition;
  @NotNull String uuid;
  @NotNull String name;
  @NotNull String quote;
  @NotNull Boolean displayMembers;
  @NotNull Boolean lookingForPlayers;
  @NotNull TeamType type;
  @NotNull TimestampsDto timestampsDto;
  @NotNull DeletionDto deletionDto;
  List<PublicPlayerDto> members;

  @NotNull
  @Min(0)
  Integer numMembers;

  public static PublicTeamDto from(Team team, List<Player> members) {
    List<PublicPlayerDto> visibleMembers =
        members != null && team.isDisplayMembers()
            ? members.stream().map(PublicPlayerDto::from).toList()
            : null;
    return from(team, visibleMembers, members != null ? members.size() : 0);
  }

  public static PublicTeamDto fromRecruitingTeam(Team team, List<Player> members) {
    List<PublicPlayerDto> recruitingMembers =
        members.stream().map(PublicPlayerDto::fromRecruitingMember).toList();
    return from(team, recruitingMembers, members.size());
  }

  private static PublicTeamDto from(Team team, List<PublicPlayerDto> members, int numberOfMembers) {
    return PublicTeamDto.builder()
        .competition(CompetitionDto.from(team.getCompetition()))
        .uuid(team.getUuid().toString())
        .name(team.getName())
        .quote(team.getQuote())
        .displayMembers(team.isDisplayMembers())
        .lookingForPlayers(team.isLookingForPlayers())
        .type(team.getType())
        .members(members)
        .numMembers(numberOfMembers)
        .timestampsDto(TimestampsDto.from(team))
        .deletionDto(DeletionDto.from(team))
        .build();
  }
}
