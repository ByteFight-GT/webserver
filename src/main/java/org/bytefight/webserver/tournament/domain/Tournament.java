package org.bytefight.webserver.tournament.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytefight.webserver.competition.domain.Competition;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

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

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    /**
     * Initializes timestamps and UUID on insert.
     */
    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now(clock);
        if (status == null) {
            status = TournamentStatus.DRAFT;
        }
        uuid = UUID.randomUUID();
    }
}
