package org.bytefight.webserver.notification;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.notification.application.NotificationService;
import org.bytefight.webserver.notification.domain.Notification;
import org.bytefight.webserver.notification.domain.NotificationPriority;
import org.bytefight.webserver.notification.domain.dto.NotificationDto;
import org.bytefight.webserver.notification.infra.NotificationRepository;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Boundary checks against a fixed {@link Clock}. Postgres stores microseconds, so instants are
 * re-read from the database before being used as boundaries.
 */
@Transactional
class NotificationServiceTest extends FullStackIntegrationTestBase {
  /** Later than any real createdAt, so every notification here was created "in the past". */
  private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");

  @TestBean private Clock clock;

  @Autowired private NotificationService notificationService;

  @Autowired private NotificationRepository notificationRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private EntityManager entityManager;

  static Clock clock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  @Test
  void expiringExactlyNowIsHiddenAndOneMicroLaterIsShown() {
    User user = testDataFactory.createUser();
    testDataFactory.createNotification("At now", null, NotificationPriority.URGENT, NOW);
    testDataFactory.createNotification(
        "Just after", null, NotificationPriority.URGENT, NOW.plus(1, ChronoUnit.MICROS));

    assertThat(notificationService.getFeed(user, 0, 10).map(NotificationDto::getTitle))
        .containsExactly("Just after");
    assertThat(notificationService.getActiveUrgent(user))
        .extracting(NotificationDto::getTitle)
        .containsExactly("Just after");
    assertThat(notificationService.getPublicUrgent())
        .extracting(NotificationDto::getTitle)
        .containsExactly("Just after");
  }

  @Test
  void createdExactlyAtLastCheckedIsReadAndOneMicroLaterIsUnread() {
    User user = testDataFactory.createUser();
    Instant createdAt =
        storedCreatedAt(
            testDataFactory.createNotification("N", null, NotificationPriority.NORMAL, null));

    user = setLastChecked(user, createdAt);
    assertThat(notificationService.getUnreadCount(user)).isZero();
    assertThat(notificationService.getFeed(user, 0, 10).getContent().get(0).isUnread()).isFalse();

    user = setLastChecked(user, createdAt.minus(1, ChronoUnit.MICROS));
    assertThat(notificationService.getUnreadCount(user)).isEqualTo(1);
    assertThat(notificationService.getFeed(user, 0, 10).getContent().get(0).isUnread()).isTrue();
  }

  @Test
  void markReadUsesTheClock() {
    User user = testDataFactory.createUser();

    notificationService.markRead(user);

    entityManager.flush();
    entityManager.clear();
    assertThat(userRepository.findById(user.getId()).orElseThrow().getLastNotificationsChecked())
        .isEqualTo(NOW);
  }

  private Instant storedCreatedAt(Notification notification) {
    entityManager.flush();
    entityManager.clear();
    return notificationRepository.findById(notification.getId()).orElseThrow().getCreatedAt();
  }

  private User setLastChecked(User user, Instant lastChecked) {
    User managed = userRepository.findById(user.getId()).orElseThrow();
    managed.setLastNotificationsChecked(lastChecked);
    return userRepository.saveAndFlush(managed);
  }
}
