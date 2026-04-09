package org.bytefight.webserver.tournament.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.gamematch.domain.GameMatch;

/**
 * Represents a single game within a best-of series (TournamentMatch).
 *
 * <p>Why this table: - A TournamentMatch is now a series (Bo5 or Bo7), not a single game. - Each
 * individual game played in the series gets a TournamentGame row. - Links the TournamentMatch
 * (series) to a GameMatch (individual game). - Tracks game number within the series for
 * ordering/display. - Enables lookup from GameMatch -> TournamentMatch via this join entity.
 */
@Entity
@Table(
    name = "tournament_game",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tournament_game_match_game_number",
          columnNames = {"tournament_match_id", "game_number"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentGame extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_match_id", nullable = false)
  private TournamentMatch tournamentMatch;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "game_match_id", nullable = false, unique = true)
  private GameMatch gameMatch;

  @Column(name = "game_number")
  private Integer gameNumber;

  @Column(name = "result_processed", nullable = false)
  @Builder.Default
  private boolean resultProcessed = false;
}
