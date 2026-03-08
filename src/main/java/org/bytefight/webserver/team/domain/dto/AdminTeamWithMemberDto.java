package org.bytefight.webserver.team.domain.dto;

import lombok.Value;

import java.util.List;

import org.bytefight.webserver.common.domain.dto.DeletionDto;
import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMemberDetails;
import org.bytefight.webserver.team.domain.TeamType;

import com.fasterxml.jackson.annotation.JsonProperty;

@Value
public class AdminTeamWithMemberDto {
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
  DeletionDto deletion;
  List<MemberDto> members;

  public static AdminTeamWithMemberDto from(Team team) {
    return from(team, List.of());
  }

  public static AdminTeamWithMemberDto from(Team team, List<MemberDto> members) {
    return new AdminTeamWithMemberDto(
        team.getId(),
        team.getUuid().toString(),
        team.getCompetition().getId(),
        team.getName(),
        team.getQuote(),
        team.getJoinCode(),
        team.isDisplayMembers(),
        team.getType(),
        team.isDeleted(),
        TimestampsDto.from(team),
        DeletionDto.from(team),
        members);
  }

  @Value
  public static class MemberDto {
    @JsonProperty("PlayerID")
    Long playerId;

    @JsonProperty("PlayerUserID")
    Long playerUserId;

    @JsonProperty("Playeruuid")
    String playerUuid;

    @JsonProperty("PlayerUsername")
    String playerUsername;

    @JsonProperty("PlayerEmail")
    String playerEmail;

    public static MemberDto from(TeamMemberDetails member) {
      return new MemberDto(
          member.getPlayerId(),
          member.getPlayerUserId(),
          member.getPlayerUuid().toString(),
          member.getPlayerUsername(),
          member.getPlayerEmail());
    }
  }
}
