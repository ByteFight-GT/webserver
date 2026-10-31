package org.bytefight.webserver.notification.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import org.bytefight.webserver.notification.domain.NotificationPriority;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * PATCH semantics: a field left out of the request is not changed.
 *
 * <p>{@code competitionId} and {@code expireAt} are nullable on the entity, so an explicit JSON
 * null has to mean "clear it" (make the notification site-wide or never-expiring) while an absent
 * field means "leave it". Jackson only calls a setter for fields present in the body, so the
 * setters record presence.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminUpdateNotificationDto {
  @Size(min = 1, max = 200)
  private String title;

  @Size(min = 1)
  private String body;

  private NotificationPriority priority;
  private Boolean withEmail;
  private Long competitionId;
  private Instant expireAt;

  @JsonIgnore private boolean competitionIdPresent;
  @JsonIgnore private boolean expireAtPresent;

  public void setCompetitionId(Long competitionId) {
    this.competitionId = competitionId;
    this.competitionIdPresent = true;
  }

  public void setExpireAt(Instant expireAt) {
    this.expireAt = expireAt;
    this.expireAtPresent = true;
  }
}
