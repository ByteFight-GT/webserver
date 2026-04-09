package org.bytefight.webserver.tournament.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bracket node table — now represents a best-of SERIES, not a single game.
 *
 * <p>Why this table: - Represents a single match node in the double-elimination graph. - Each node
 * is a best-of series (Bo5 for regular matches, Bo7 for grand finals). - Stores bracket metadata
 * (round, index, bracket type). - Encodes graph edges via nextWinnerMatchId/nextLoserMatchId. -
 * Individual games within the series are tracked by TournamentGame entities. - The series is
 * decided when one side reaches (seriesLength + 1) / 2 wins.
 *
 * <p>State flow: PENDING -> QUEUED (first game queued, both teams present) QUEUED -> QUEUED (game
 * finishes, series not decided, next game queued) QUEUED -> COMPLETE (game finishes, series
 * decided) PENDING -> SKIPPED (bye — one or both teams missing)
 */
@Entity
@Table(name = "tournament_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch extends BaseEntity {

  @Column(name = "uuid", nullable = false, unique = true, updatable = false)
  @Builder.Default
  private UUID uuid = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "bracket_type", columnDefinition = "tournament_bracket_type")
  private TournamentBracketType bracketType;

  @Column(name = "round_number")
  private Integer roundNumber;

  @Column(name = "match_index")
  private Integer matchIndex;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_one_entry_id")
  private TournamentEntry teamOneEntry;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_two_entry_id")
  private TournamentEntry teamTwoEntry;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "state", nullable = false, columnDefinition = "tournament_match_state")
  @Builder.Default
  private TournamentMatchState state = TournamentMatchState.PENDING;

  // ── Series tracking fields ──────────────────────────────────────────────
  // These replace the old single-game `gameMatch` FK.

  /**
   * Maximum games in this series (5 for regular matches, 7 for grand finals). The wins threshold to
   * decide the series is (seriesLength + 1) / 2.
   */
  private Integer seriesLength;

  @Column(name = "team_one_series_wins", nullable = false)
  @Builder.Default
  private Integer teamOneSeriesWins = 0;

  @Column(name = "team_two_series_wins", nullable = false)
  @Builder.Default
  private Integer teamTwoSeriesWins = 0;

  /**
   * Individual games in this series, ordered by game number. Populated by TournamentGame entities
   * (the join between this series and GameMatch).
   */
  @OneToMany(mappedBy = "tournamentMatch", fetch = FetchType.LAZY)
  @OrderBy("gameNumber ASC")
  @Builder.Default
  private List<TournamentGame> games = new ArrayList<>();

  // ── Series winner/loser (set when series is decided) ────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_entry_id")
  private TournamentEntry winnerEntry;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "loser_entry_id")
  private TournamentEntry loserEntry;

  @Column(name = "next_winner_match_id")
  private Long nextWinnerMatchId;

  @Column(name = "next_winner_slot")
  private Integer nextWinnerSlot;

  @Column(name = "next_loser_match_id")
  private Long nextLoserMatchId;

  @Column(name = "next_loser_slot")
  private Integer nextLoserSlot;

  public int getWinsRequired() {
    return (seriesLength + 1) / 2;
  }

  public boolean isSeriesDecided() {
    int required = getWinsRequired();
    return teamOneSeriesWins >= required || teamTwoSeriesWins >= required;
  }
}
