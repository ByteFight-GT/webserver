package org.bytefight.webserver.tournament.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Read-only match node view for bracket/timeline rendering.
 *
 * <p>Key changes from single-game model: - No longer has a single gameMatchUuid. Instead, the
 * series is represented by seriesLength, teamOneSeriesWins, teamTwoSeriesWins, and a list of
 * individual games. - Each game in the series is a TournamentGameDto with its own GameMatch UUID. -
 * The frontend can render the series score (e.g., "3-1") and link to individual game logs.
 */
@Getter
@Builder
public class TournamentMatchDto {
  private final Long matchId;
  private final String uuid;
  private final TournamentBracketType bracketType;
  private final Integer roundNumber;
  private final Integer matchIndex;
  private final Long teamOneEntryId;
  private final String teamOneUuid;
  private final String teamOneName;
  private final Long teamTwoEntryId;
  private final String teamTwoUuid;
  private final String teamTwoName;
  private final TournamentMatchState state;

  // ── Series fields ───────────────────────────────────────────────────────

  /** Max games in this series (5 for normal, 7 for grand finals). */
  private final Integer seriesLength;

  /** Wins by team one so far in the series. */
  private final Integer teamOneSeriesWins;

  /** Wins by team two so far in the series. */
  private final Integer teamTwoSeriesWins;

  /** Individual games played in this series, ordered by game number. */
  private final List<TournamentGameDto> games;

  // ── Bracket graph fields ────────────────────────────────────────────────

  private final Long winnerEntryId;
  private final Long loserEntryId;
  private final Long nextWinnerMatchId;
  private final Integer nextWinnerSlot;
  private final Long nextLoserMatchId;
  private final Integer nextLoserSlot;

  /**
   * Converts a TournamentMatch entity (series) to its DTO representation. Eagerly maps all
   * TournamentGame records within the series to TournamentGameDto.
   */
  public static TournamentMatchDto from(TournamentMatch match) {
    var teamOne = match.getTeamOneEntry() != null ? match.getTeamOneEntry().getTeam() : null;
    var teamTwo = match.getTeamTwoEntry() != null ? match.getTeamTwoEntry().getTeam() : null;

    List<TournamentGameDto> gameDtos =
        match.getGames() != null
            ? match.getGames().stream().map(TournamentGameDto::from).toList()
            : List.of();

    return TournamentMatchDto.builder()
        .matchId(match.getId())
        .uuid(match.getUuid().toString())
        .bracketType(match.getBracketType())
        .roundNumber(match.getRoundNumber())
        .matchIndex(match.getMatchIndex())
        .teamOneEntryId(match.getTeamOneEntry() != null ? match.getTeamOneEntry().getId() : null)
        .teamOneUuid(teamOne != null ? teamOne.getUuid().toString() : null)
        .teamOneName(teamOne != null ? teamOne.getName() : null)
        .teamTwoEntryId(match.getTeamTwoEntry() != null ? match.getTeamTwoEntry().getId() : null)
        .teamTwoUuid(teamTwo != null ? teamTwo.getUuid().toString() : null)
        .teamTwoName(teamTwo != null ? teamTwo.getName() : null)
        .state(match.getState())
        .seriesLength(match.getSeriesLength())
        .teamOneSeriesWins(match.getTeamOneSeriesWins())
        .teamTwoSeriesWins(match.getTeamTwoSeriesWins())
        .games(gameDtos)
        .winnerEntryId(match.getWinnerEntry() != null ? match.getWinnerEntry().getId() : null)
        .loserEntryId(match.getLoserEntry() != null ? match.getLoserEntry().getId() : null)
        .nextWinnerMatchId(match.getNextWinnerMatchId())
        .nextWinnerSlot(match.getNextWinnerSlot())
        .nextLoserMatchId(match.getNextLoserMatchId())
        .nextLoserSlot(match.getNextLoserSlot())
        .build();
  }
}
