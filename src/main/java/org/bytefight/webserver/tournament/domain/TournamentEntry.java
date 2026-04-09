package org.bytefight.webserver.tournament.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.team.domain.Team;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Tournament participant row.
 *
 * <p>Why this table: - Links a Team to a Tournament - Tracks seed and current loss count - Enables
 * double-elimination logic without mutating Team records
 */
@Entity
@Table(
    name = "tournament_entry",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tournament_entry_tournament_team",
          columnNames = {"tournament_id", "team_id"})
    })
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

  @Column(name = "seed")
  private Integer seed;

  @Column(name = "losses", nullable = false)
  @Builder.Default
  private Integer losses = 0;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", nullable = false, columnDefinition = "tournament_entry_status")
  @Builder.Default
  private TournamentEntryStatus status = TournamentEntryStatus.ACTIVE;

  @Column(name = "eliminated_at")
  private LocalDateTime eliminatedAt;
}
