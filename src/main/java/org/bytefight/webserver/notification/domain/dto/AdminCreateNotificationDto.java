package org.bytefight.webserver.notification.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.time.Instant;

import org.bytefight.webserver.notification.domain.NotificationPriority;

@Value
public class AdminCreateNotificationDto {
  @NotBlank
  @Size(max = 200)
  String title;

  /** Markdown. */
  @NotBlank String body;

  /** Defaults to NORMAL. */
  NotificationPriority priority;

  /** Defaults to false. Stored only; email delivery is not implemented yet. */
  Boolean withEmail;

  /** Null means site-wide. */
  Long competitionId;

  /** Null means never expires. Must be in the future when set. */
  Instant expireAt;
}
