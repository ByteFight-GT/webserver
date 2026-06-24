package org.bytefight.webserver.player.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.storage.domain.FileRecord;
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

  @Column(name = "major", length=256)
  private String major;

  @Column(name = "graduation_year")
  private Integer graduationYear;

  @Column(name = "school", length = 150)
  private String school;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "avatar_file", unique = true)
  private FileRecord avatar;

  @OneToMany(
    mappedBy = "player",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  private List<SocialLink> socialLinks = new ArrayList<SocialLink>();

  @Enumerated(EnumType.STRING)
  @Column(name = "profile_visibility", nullable = false)
  private ProfileVisibility profileVisibility = ProfileVisibility.PRIVATE;
}
