package org.bytefight.webserver.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.player.domain.Player;

@Entity
@Table(
    name = "player_profiles",
    indexes = {
      @Index(name = "idx_player_profiles_major", columnList = "major"),
      @Index(name = "idx_player_profiles_school", columnList = "school"),
      @Index(name = "idx_player_profiles_graduation_year", columnList = "graduation_year")
    })
@Getter
@Setter
@NoArgsConstructor
public class Profile extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false, unique = true)
  private Player player;

  @Column(name = "description", length = 512)
  private String description;

  @Column(name = "major", length = 256)
  private String major;

  @Column(name = "school", length = 150)
  private String school;

  @Column(name = "graduation_year")
  private Integer graduationYear;
}
