package org.bytefight.webserver.notification.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.Instant;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.notification.domain.Notification;
import org.bytefight.webserver.notification.domain.NotificationPriority;
import org.bytefight.webserver.user.domain.User;

@Value
public class AdminNotificationDto {
  @NotNull Long id;
  @NotNull String title;
  @NotNull String body;
  @NotNull NotificationPriority priority;
  @NotNull boolean withEmail;
  Long competitionId;
  Instant expireAt;
  @NotNull Instant createdAt;
  @NotNull Instant updatedAt;
  Long createdByUserId;
  String createdByEmail;
  Long updatedByUserId;
  String updatedByEmail;

  public static AdminNotificationDto from(Notification notification) {
    Competition competition = notification.getCompetition();
    User createdBy = notification.getCreatedByUser();
    User updatedBy = notification.getUpdatedByUser();
    return new AdminNotificationDto(
        notification.getId(),
        notification.getTitle(),
        notification.getBody(),
        notification.getPriority(),
        notification.isWithEmail(),
        competition == null ? null : competition.getId(),
        notification.getExpireAt(),
        notification.getCreatedAt(),
        notification.getUpdatedAt(),
        createdBy == null ? null : createdBy.getId(),
        createdBy == null ? null : createdBy.getEmail(),
        updatedBy == null ? null : updatedBy.getId(),
        updatedBy == null ? null : updatedBy.getEmail());
  }
}
