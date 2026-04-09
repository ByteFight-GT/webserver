package org.bytefight.webserver.tournament.domain;

import lombok.Builder;
import lombok.Getter;

import org.bytefight.webserver.team.domain.Team;

/** Read-only participant data for bracket rendering. */
@Getter
@Builder
public class TournamentEntryDto {
  private final Long entryId;
  private final String teamUuid;
  private final String teamName;
  private final Integer seed;
  private final Integer losses;
  private final TournamentEntryStatus status;

  public static TournamentEntryDto from(TournamentEntry entry) {
    Team team = entry.getTeam();
    return TournamentEntryDto.builder()
        .entryId(entry.getId())
        .teamUuid(team.getUuid().toString())
        .teamName(team.getName())
        .seed(entry.getSeed())
        .losses(entry.getLosses())
        .status(entry.getStatus())
        .build();
  }
}
