package org.bytefight.webserver.player.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.common.domain.BaseEntity;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "players")
public class Player extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "username", nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "username_normalized", nullable = false, unique = true, length = 50)
  private String usernameNormalized;
}
