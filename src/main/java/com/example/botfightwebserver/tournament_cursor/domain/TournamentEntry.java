package com.example.botfightwebserver.tournament_cursor.domain;

import com.example.botfightwebserver.team.domain.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Tournament participant row.
 *
 * Why this table:
 * - Links a Team to a Tournament
 * - Tracks seed and current loss count
 * - Enables double-elimination logic without mutating Team records
 */
@Entity
@Table(
        name = "tournament_cursor_entry",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tournament_id", "team_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    private Integer seed;
    private Integer losses;

    @Enumerated(EnumType.STRING)
    private TournamentEntryStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime eliminatedAt;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    /**
     * Initializes timestamps, losses, and status on insert.
     */
    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now(clock);
        if (losses == null) {
            losses = 0;
        }
        if (status == null) {
            status = TournamentEntryStatus.ACTIVE;
        }
    }
}
