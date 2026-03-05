package org.bytefight.webserver.competition.domain.dto;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.competition.domain.Competition;

public record AdminCompetitionDto(
    Long id,
    String slug,
    String name,
    String description,
    boolean isActive,
    boolean isWhitelisted,
    int maxPlayersPerTeam,
    long teamSubmissionStorageSize,
    TimestampsDto timestamps,
    boolean allowNewSubmission,
    boolean allowSetSubmission,
    boolean allowCreateTeam,
    boolean allowJoinTeam,
    boolean allowLeaveTeam,
    boolean allowEditTeamName) {
  public static AdminCompetitionDto from(Competition competition) {
    return new AdminCompetitionDto(
        competition.getId(),
        competition.getSlug(),
        competition.getName(),
        competition.getDescription(),
        competition.isActive(),
        competition.isWhitelisted(),
        competition.getMaxPlayersPerTeam(),
        competition.getTeamSubmissionStorageSize(),
        TimestampsDto.from(competition),
        competition.isAllowNewSubmission(),
        competition.isAllowSetSubmission(),
        competition.isAllowCreateTeam(),
        competition.isAllowJoinTeam(),
        competition.isAllowLeaveTeam(),
        competition.isAllowEditTeamName());
  }
}
