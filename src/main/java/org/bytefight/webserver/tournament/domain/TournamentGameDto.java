package org.bytefight.webserver.tournament.domain;

import lombok.Builder;
import lombok.Getter;

import org.bytefight.webserver.gamematch.domain.GameMatch;

// import org.bytefight.webserver.gamematch.domain.MatchStatus;

/**
 * Read-only DTO for a single game within a best-of series.
 *
 * <p>Each TournamentMatch (series) contains multiple TournamentGame records. This DTO exposes the
 * game number, the underlying GameMatch UUID (for linking to game logs/replays), and the result
 * status of that game.
 */
@Getter
@Builder
public class TournamentGameDto {
  /** Sequential game number within the series (1-based). */
  private final Integer gameNumber;

  /** UUID of the underlying GameMatch (for linking to logs/replays). */
  private final String gameMatchUuid;

  /** Current status of the underlying GameMatch (queued, in_progress, team_a_win, etc.). */
  private final String gameMatchStatus;

  public static TournamentGameDto from(TournamentGame game) {
    GameMatch gm = game.getGameMatch();
    return TournamentGameDto.builder()
        .gameNumber(game.getGameNumber())
        .gameMatchUuid(gm != null ? gm.getUuid().toString() : null)
        .gameMatchStatus(gm != null && gm.getStatus() != null ? gm.getStatus().name() : null)
        .build();
  }
}
