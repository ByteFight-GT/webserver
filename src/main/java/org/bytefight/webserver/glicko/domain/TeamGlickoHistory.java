package org.bytefight.webserver.glicko.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.team.domain.Team;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(
    name = "team_glicko_history",
    indexes = {
      @Index(name = "ix_team_glicko_history_team_match", columnList = "team_id,game_match_id")
    })
public class TeamGlickoHistory extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competition_id", nullable = false)
  private Competition competition;

  @Column(name = "ladder", nullable = false, length = 50)
  private String ladder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_match_id")
  private GameMatch gameMatch;

  @Column(name = "old_glicko", nullable = false)
  private double oldGlicko;

  @Column(name = "new_glicko", nullable = false)
  private double newGlicko;

  public void setTeam(Team team) {
    this.team = team;
  }

  public void setCompetition(Competition competition) {
    this.competition = competition;
  }

  public void setLadder(String ladder) {
    this.ladder = ladder;
  }

  public void setGameMatch(GameMatch gameMatch) {
    this.gameMatch = gameMatch;
  }

  public void setOldGlicko(double oldGlicko) {
    this.oldGlicko = oldGlicko;
  }

  public void setNewGlicko(double newGlicko) {
    this.newGlicko = newGlicko;
  }
}
