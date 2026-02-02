package org.bytefight.webserver.tournament.domain;

import org.bytefight.webserver.gameMatch.domain.GameMatch;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Bracket node table.
 *
 * Why this table:
 * - Represents a single match node in the double-elimination graph
 * - Stores bracket metadata (round, index, bracket type)
 * - Encodes graph edges via nextWinnerMatchId/nextLoserMatchId
 * - Links to an actual GameMatch when queued/executed
 */
@Entity
@Table(name = "tournament_cursor_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_match_id")
    private GameMatch gameMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_entry_id")
    private TournamentEntry winnerEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loser_entry_id")
    private TournamentEntry loserEntry;

    private Long nextWinnerMatchId;
    private Integer nextWinnerSlot;
    private Long nextLoserMatchId;
    private Integer nextLoserSlot;

    private LocalDateTime createdAt;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    /**
     * Initializes timestamps, state, and UUID on insert.
     */
    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now(clock);
        if (state == null) {
            state = TournamentMatchState.PENDING;
        }
        uuid = UUID.randomUUID();
    }
}
