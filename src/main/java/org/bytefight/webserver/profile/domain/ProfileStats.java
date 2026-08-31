package org.bytefight.webserver.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.player.domain.Player;

@Getter
@Setter
@Entity
@Table(
    name = "profile_stats",
    indexes = {@Index(name = "uk_profile_stats_player", columnList = "player_id", unique = true)})
public class ProfileStats extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false, unique = true)
  private Player player;

  @Column(name = "games_played", nullable = false)
  private int gamesPlayed;

  @Column(name = "games_played_percentile")
  private Double gamesPlayedPercentile;
}
