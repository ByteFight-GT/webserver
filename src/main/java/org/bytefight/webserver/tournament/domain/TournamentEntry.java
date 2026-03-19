package org.bytefight.webserver.tournament.domain;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.team.domain.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
        name = "tournament_entry",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tournament_id", "team_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentEntry extends BaseEntity {

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

    private LocalDateTime eliminatedAt;

    /**
     * Initializes losses and status on insert.
     */
    @PrePersist
    public void onCreate() {
        if (losses == null) {
            losses = 0;
        }
        if (status == null) {
            status = TournamentEntryStatus.ACTIVE;
        }
    }
}
