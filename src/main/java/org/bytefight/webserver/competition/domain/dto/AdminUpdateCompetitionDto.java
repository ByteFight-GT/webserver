package org.bytefight.webserver.competition.domain.dto;

import jakarta.validation.constraints.Min;
import java.util.Map;
import lombok.Value;

@Value
public class AdminUpdateCompetitionDto {
  String name;
  String description;
  Boolean isActive;
  Boolean isWhitelisted;
  Boolean allowNewSubmission;
  Boolean allowSetSubmission;
  Boolean allowCreateTeam;
  Boolean allowJoinTeam;
  Boolean allowLeaveTeam;
  Boolean allowEditTeamName;

  @Min(1)
  Integer maxPlayersPerTeam;

  @Min(0)
  Long teamSubmissionStorageSize;

  Map<String, Object> settings;
}
