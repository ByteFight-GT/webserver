package org.bytefight.webserver.tournament.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * Read-only DTO for a team's final tournament placement.
 *
 * <p>Returned by the rankings endpoint once a tournament is COMPLETE. Teams are ranked by: 1st/2nd
 * place -> stored directly on Tournament Remaining -> losers-bracket elimination round (later round
 * = better; same round = tied rank)
 */
@Getter
@Builder
public class TournamentRankingDto {
  /** 1-based placement (1 = champion, 2 = runner-up, etc.). */
  private final Integer rank;

  private final Long entryId;
  private final String teamUuid;
  private final String teamName;

  /** Original seed the team was given at enrollment. */
  private final Integer seed;

  /** Total bracket-level losses (0, 1, or 2 in double elimination). */
  private final Integer losses;

  /** ACTIVE or ELIMINATED. */
  private final TournamentEntryStatus status;
}
