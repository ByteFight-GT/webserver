package org.bytefight.webserver.tournament.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.competition.domain.Competition;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Root tournament table.
 *
 * <p>Why this table: - Represents a single tournament event - Holds global metadata (status,
 * bracket size) - Acts as the parent for entries and matches - Scoped to a single competition
 * (multi-competition support)
 */
@Entity
@Table(name = "tournament")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament extends BaseEntity {

  @Column(name = "uuid", nullable = false, unique = true, updatable = false)
  @Builder.Default
  private UUID uuid = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competition_id", nullable = false)
  private Competition competition;

  @Column(name = "name", nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", nullable = false, columnDefinition = "tournament_status")
  @Builder.Default
  private TournamentStatus status = TournamentStatus.DRAFT;

  @Column(name = "bracket_size")
  private Integer bracketSize;

  // ── Final standings (set when tournament completes) ──────────────────────

  /**
   * The tournament champion (1st place). Set when the grand final or grand-final reset concludes.
   * OneToOne: each entry belongs to one tournament and can only be first place there.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "first_place_entry_id", unique = true)
  private TournamentEntry firstPlaceEntry;

  /**
   * The runner-up (2nd place). Set when the grand final or grand-final reset concludes. OneToOne:
   * each entry belongs to one tournament and can only be second place there.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "second_place_entry_id", unique = true)
  private TournamentEntry secondPlaceEntry;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;
}
