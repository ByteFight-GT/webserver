package org.bytefight.webserver.notification.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.notification.domain.Notification;
import org.bytefight.webserver.notification.domain.NotificationPriority;

/** User-facing view of a notification. The competition fields are null for site-wide ones. */
@Value
@Builder
public class NotificationDto {
  @NotNull Long id;
  @NotNull String title;

  /** Markdown. */
  @NotNull String body;

  @NotNull NotificationPriority priority;
  Long competitionId;
  String competitionSlug;
  String competitionName;
  @NotNull Instant createdAt;
  Instant expireAt;

  /** Created after the user last opened the bell. Always false on the public endpoint. */
  @NotNull boolean unread;

  public static NotificationDto from(Notification notification, Instant lastChecked) {
    Competition competition = notification.getCompetition();
    return NotificationDto.builder()
        .id(notification.getId())
        .title(notification.getTitle())
        .body(notification.getBody())
        .priority(notification.getPriority())
        .competitionId(competition == null ? null : competition.getId())
        .competitionSlug(competition == null ? null : competition.getSlug())
        .competitionName(competition == null ? null : competition.getName())
        .createdAt(notification.getCreatedAt())
        .expireAt(notification.getExpireAt())
        .unread(lastChecked != null && notification.getCreatedAt().isAfter(lastChecked))
        .build();
  }
}
