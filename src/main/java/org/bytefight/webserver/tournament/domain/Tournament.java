package org.bytefight.webserver.tournament.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.competition.domain.Competition;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Root tournament table.
 *
 * Why this table:
 * - Represents a single tournament event
 * - Holds global metadata (status, max teams, bracket size)
 * - Acts as the parent for entries and matches
 * - Scoped to a single competition (multi-competition support)
 */
@Entity
@Table(name = "tournament")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament extends BaseEntity {

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    private Integer maxTeams;
    private Integer bracketSize;

    // ── Final standings (set when tournament completes) ──────────────────────

    /**
     * The tournament champion (1st place).
     * Set when the grand final or grand-final reset concludes.
     * OneToOne: each entry belongs to one tournament and can only be first place there.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_place_entry_id", unique = true)
    private TournamentEntry firstPlaceEntry;

    /**
     * The runner-up (2nd place).
     * Set when the grand final or grand-final reset concludes.
     * OneToOne: each entry belongs to one tournament and can only be second place there.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "second_place_entry_id", unique = true)
    private TournamentEntry secondPlaceEntry;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    /**
     * Initializes default status and UUID on insert.
     */
    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = TournamentStatus.DRAFT;
        }
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
