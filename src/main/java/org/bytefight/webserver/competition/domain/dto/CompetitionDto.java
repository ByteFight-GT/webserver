package org.bytefight.webserver.competition.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.competition.domain.Competition;

@Value
@Builder
public class CompetitionDto {
  @NotNull String slug;
  @NotNull String name;
  String description;
  @NotNull boolean isActive;
  @NotNull boolean isWhitelisted;
  @NotNull boolean allowNewSubmission;
  @NotNull boolean allowSetSubmission;
  @NotNull boolean allowCreateTeam;
  @NotNull boolean allowJoinTeam;
  @NotNull boolean allowLeaveTeam;
  @NotNull boolean allowEditTeamName;
  @NotNull boolean allowCreateUserMatch;
  @NotNull Map<String, Object> settings;
  @NotNull TimestampsDto timestamps;

  public static CompetitionDto from(Competition competition) {
    return CompetitionDto.builder()
        .slug(competition.getSlug())
        .name(competition.getName())
        .description(competition.getDescription())
        .isActive(competition.isActive())
        .isWhitelisted(competition.isWhitelisted())
        .allowNewSubmission(competition.isAllowNewSubmission())
        .allowSetSubmission(competition.isAllowSetSubmission())
        .allowCreateTeam(competition.isAllowCreateTeam())
        .allowJoinTeam(competition.isAllowJoinTeam())
        .allowLeaveTeam(competition.isAllowLeaveTeam())
        .allowEditTeamName(competition.isAllowEditTeamName())
        .allowCreateUserMatch(competition.isAllowCreateUserMatch())
        .settings(competition.getSettings())
        .timestamps(TimestampsDto.from(competition))
        .build();
  }
}
