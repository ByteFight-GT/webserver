package org.bytefight.webserver.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import org.bytefight.webserver.common.domain.AuditableEntity;
import org.bytefight.webserver.competition.domain.Competition;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Admin-authored announcement. A null competition makes it site-wide; otherwise only players on a
 * team in that competition see it. Unread state is per user, via {@code
 * User.lastNotificationsChecked} compared against {@code createdAt}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends AuditableEntity {
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "priority", nullable = false, columnDefinition = "notification_priority")
  @Builder.Default
  private NotificationPriority priority = NotificationPriority.NORMAL;

  /** Stored for the email follow-up; nothing sends email yet. */
  @Column(name = "with_email", nullable = false)
  private boolean withEmail;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  /** Markdown. */
  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  /** Null means site-wide. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "competition_id")
  private Competition competition;

  /** Null means it never expires. Expired notifications are hidden from users, not admins. */
  @Column(name = "expire_at")
  private Instant expireAt;
}
