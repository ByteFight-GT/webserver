package org.bytefight.webserver.competition.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "competitions")
public class Competition extends BaseEntity {
  @Column(name = "slug", nullable = false, unique = true, length = 255)
  private String slug;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = false;

  @Column(name = "is_whitelisted", nullable = false)
  private boolean isWhitelisted = false;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "settings", nullable = false)
  private Map<String, Object> settings = Map.of();

  @Column(name = "max_players_per_team", nullable = false)
  private int maxPlayersPerTeam = 2;

  @Column(name = "team_submission_storage_size", nullable = false)
  private long teamSubmissionStorageSize = 0;

  @Column(name = "allow_new_submission", nullable = false)
  private boolean allowNewSubmission;

  @Column(name = "allow_set_submission", nullable = false)
  private boolean allowSetSubmission;

  @Column(name = "allow_create_team", nullable = false)
  private boolean allowCreateTeam;

  @Column(name = "allow_join_team", nullable = false)
  private boolean allowJoinTeam;

  @Column(name = "allow_leave_team", nullable = false)
  private boolean allowLeaveTeam;

  @Column(name = "allow_edit_team_name", nullable = false)
  private boolean allowEditTeamName;

  @Column(name = "allow_create_user_match", nullable = false)
  private boolean allowCreateUserMatch;

  @Override
  public int hashCode() {
    return Objects.hashCode(slug);
  }

  public String getSlug() {
    return slug;
  }
}
