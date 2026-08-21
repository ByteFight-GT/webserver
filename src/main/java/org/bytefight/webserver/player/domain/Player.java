package org.bytefight.webserver.player.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.user.domain.User;

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

  @Column(name = "full_name", length = 100)
  private String fullName;

  @Column(name = "description", length = 512)
  private String description;

  @Column(name = "school", length = 150)
  private String school;

  @Column(name = "major", length = 256)
  private String major;

  @Column(name = "github_link", length = 500)
  private String githubLink;

  @Column(name = "linkedin_link", length = 500)
  private String linkedinLink;

  @Column(name = "website_link", length = 500)
  private String websiteLink;
}
