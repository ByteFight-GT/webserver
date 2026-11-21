package org.bytefight.webserver.competition.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Value;

import java.util.Map;

import org.bytefight.webserver.competition.domain.Competition;

@Value
public class AdminUpdateCompetitionDto {
  String name;
  String description;
  Boolean isActive;
  Boolean isWhitelisted;
  Boolean internal;
  Boolean allowNewSubmission;
  Boolean allowSetSubmission;
  Boolean allowCreateTeam;
  Boolean allowJoinTeam;
  Boolean allowLeaveTeam;
  Boolean allowEditTeamName;
  Boolean allowCreateUserMatch;
  Boolean leaderboardPagination;

  @Min(1)
  @Max(Competition.MAX_LEADERBOARD_PAGE_SIZE)
  Integer leaderboardPageSize;

  @Min(1)
  Integer maxPlayersPerTeam;

  @Min(0)
  Long teamSubmissionStorageSize;

  Map<String, Object> settings;
}
