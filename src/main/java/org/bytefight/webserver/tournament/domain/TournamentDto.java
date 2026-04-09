package org.bytefight.webserver.tournament.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

import org.bytefight.webserver.team.domain.Team;

/**
 * Read-only tournament metadata for API responses, scoped to a competition.
 *
 * <p>Includes final standings (1st and 2nd place) when the tournament is complete. These are
 * populated from the grand final or grand-final reset result.
 */
@Getter
@Builder
public class TournamentDto {
  private final String uuid;
  private final String competitionSlug;
  private final String name;
  private final TournamentStatus status;
  private final Integer bracketSize;
  private final Instant createdAt;
  private final LocalDateTime startedAt;
  private final LocalDateTime finishedAt;

  // ── Final standings ─────────────────────────────────────────────────────

  /** 1st place entry ID (champion). Null until tournament completes. */
  private final Long firstPlaceEntryId;

  /** 1st place team UUID (for linking to team profile). */
  private final String firstPlaceTeamUuid;

  /** 1st place team display name. */
  private final String firstPlaceTeamName;

  /** 2nd place entry ID (runner-up). Null until tournament completes. */
  private final Long secondPlaceEntryId;

  /** 2nd place team UUID. */
  private final String secondPlaceTeamUuid;

  /** 2nd place team display name. */
  private final String secondPlaceTeamName;

  public static TournamentDto from(Tournament tournament) {
    TournamentEntry first = tournament.getFirstPlaceEntry();
    TournamentEntry second = tournament.getSecondPlaceEntry();
    Team firstTeam = first != null ? first.getTeam() : null;
    Team secondTeam = second != null ? second.getTeam() : null;

    return TournamentDto.builder()
        .uuid(tournament.getUuid().toString())
        .competitionSlug(tournament.getCompetition().getSlug())
        .name(tournament.getName())
        .status(tournament.getStatus())
        .bracketSize(tournament.getBracketSize())
        .createdAt(tournament.getCreatedAt())
        .startedAt(tournament.getStartedAt())
        .finishedAt(tournament.getFinishedAt())
        .firstPlaceEntryId(first != null ? first.getId() : null)
        .firstPlaceTeamUuid(firstTeam != null ? firstTeam.getUuid().toString() : null)
        .firstPlaceTeamName(firstTeam != null ? firstTeam.getName() : null)
        .secondPlaceEntryId(second != null ? second.getId() : null)
        .secondPlaceTeamUuid(secondTeam != null ? secondTeam.getUuid().toString() : null)
        .secondPlaceTeamName(secondTeam != null ? secondTeam.getName() : null)
        .build();
  }
}
