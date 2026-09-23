package org.bytefight.webserver.gamematch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.competition.domain.Competition;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "game_match_outcome_reasons",
    uniqueConstraints = @UniqueConstraint(columnNames = {"competition_id", "code"}))
public class GameOutcomeReason extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competition_id", nullable = false)
  private Competition competition;

  @Column(name = "code", nullable = false, length = 100)
  private String code;

  @Column(name = "display_label", nullable = false, length = 255)
  private String displayLabel;

  @Column(name = "visible", nullable = false)
  private boolean visible = true;
}
