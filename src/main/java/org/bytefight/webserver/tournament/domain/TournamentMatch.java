package org.bytefight.webserver.tournament.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytefight.webserver.common.domain.BaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bracket node table — now represents a best-of SERIES, not a single game.
 *
 * Why this table:
 * - Represents a single match node in the double-elimination graph.
 * - Each node is a best-of series (Bo5 for regular matches, Bo7 for grand finals).
 * - Stores bracket metadata (round, index, bracket type).
 * - Encodes graph edges via nextWinnerMatchId/nextLoserMatchId.
 * - Individual games within the series are tracked by TournamentGame entities.
 * - The series is decided when one side reaches (seriesLength + 1) / 2 wins.
 *
 * State flow:
 *   PENDING  -> QUEUED (first game queued, both teams present)
 *   QUEUED   -> QUEUED (game finishes, series not decided, next game queued)
 *   QUEUED   -> COMPLETE (game finishes, series decided)
 *   PENDING  -> SKIPPED (bye — one or both teams missing)
 */
@Entity
@Table(name = "tournament_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch extends BaseEntity {

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Enumerated(EnumType.STRING)
    private TournamentBracketType bracketType;

    private Integer roundNumber;
    private Integer matchIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_one_entry_id")
    private TournamentEntry teamOneEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_two_entry_id")
    private TournamentEntry teamTwoEntry;

    @Enumerated(EnumType.STRING)
    private TournamentMatchState state;

    // ── Series tracking fields ──────────────────────────────────────────────
    // These replace the old single-game `gameMatch` FK.

    /**
     * Maximum games in this series (5 for regular matches, 7 for grand finals).
     * The wins threshold to decide the series is (seriesLength + 1) / 2.
     */
    private Integer seriesLength;

    /**
     * How many individual games teamOne has won in this series so far.
     */
    @Builder.Default
    private Integer teamOneSeriesWins = 0;

    /**
     * How many individual games teamTwo has won in this series so far.
     */
    @Builder.Default
    private Integer teamTwoSeriesWins = 0;

    /**
     * Individual games in this series, ordered by game number.
     * Populated by TournamentGame entities (the join between this series and GameMatch).
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

    // ── Graph edges (bracket advancement wiring) ────────────────────────────

    private Long nextWinnerMatchId;
    private Integer nextWinnerSlot;
    private Long nextLoserMatchId;
    private Integer nextLoserSlot;

    /**
     * Initializes state, UUID, and series counters on insert.
     */
    @PrePersist
    public void onCreate() {
        if (state == null) {
            state = TournamentMatchState.PENDING;
        }
        if (teamOneSeriesWins == null) {
            teamOneSeriesWins = 0;
        }
        if (teamTwoSeriesWins == null) {
            teamTwoSeriesWins = 0;
        }
        uuid = UUID.randomUUID();
    }

    // ── Helper methods ──────────────────────────────────────────────────────

    /**
     * Returns the number of individual game wins required to win this series.
     * For Bo5: 3 wins. For Bo7: 4 wins.
     */
    public int getWinsRequired() {
        return (seriesLength + 1) / 2;
    }

    /**
     * Returns true if the series has been decided (one side has enough wins).
     */
    public boolean isSeriesDecided() {
        int required = getWinsRequired();
        return teamOneSeriesWins >= required || teamTwoSeriesWins >= required;
    }
}
