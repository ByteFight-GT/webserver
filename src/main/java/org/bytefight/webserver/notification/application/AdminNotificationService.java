package org.bytefight.webserver.notification.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.notification.domain.Notification;
import org.bytefight.webserver.notification.domain.NotificationPriority;
import org.bytefight.webserver.notification.domain.dto.AdminCreateNotificationDto;
import org.bytefight.webserver.notification.domain.dto.AdminUpdateNotificationDto;
import org.bytefight.webserver.notification.infra.NotificationRepository;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminNotificationService {
  private final NotificationRepository notificationRepository;
  private final CompetitionRepository competitionRepository;
  private final UserRepository userRepository;
  private final Clock clock;

  public Page<Notification> listNotifications(
      Specification<Notification> specification, Pageable pageable) {
    return notificationRepository.findAll(specification, pageable);
  }

  public Notification getNotification(Long id) {
    return notificationRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
  }

  public Notification createNotification(AdminCreateNotificationDto input, User actingUser) {
    if (input.getExpireAt() != null && !input.getExpireAt().isAfter(clock.instant())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expireAt must be in the future");
    }

    Notification notification =
        Notification.builder()
            .title(input.getTitle())
            .body(input.getBody())
            .priority(
                input.getPriority() == null ? NotificationPriority.NORMAL : input.getPriority())
            .withEmail(Boolean.TRUE.equals(input.getWithEmail()))
            .competition(findCompetition(input.getCompetitionId()))
            .expireAt(input.getExpireAt())
            .build();
    User actor = userReference(actingUser);
    notification.setCreatedByUser(actor);
    notification.setUpdatedByUser(actor);

    return notificationRepository.save(notification);
  }

  /**
   * Partial update. Unread state is based on {@code createdAt}, so an edit never marks a
   * notification unread again. Unlike create, {@code expireAt} may be set in the past here, which
   * is how an admin hides a notification from users without deleting it.
   */
  public Notification updateNotification(
      Long id, AdminUpdateNotificationDto input, User actingUser) {
    Notification notification = getNotification(id);

    if (input.getTitle() != null) {
      notification.setTitle(requireNotBlank(input.getTitle(), "title"));
    }
    if (input.getBody() != null) {
      notification.setBody(requireNotBlank(input.getBody(), "body"));
    }
    if (input.getPriority() != null) {
      notification.setPriority(input.getPriority());
    }
    if (input.getWithEmail() != null) {
      notification.setWithEmail(input.getWithEmail());
    }
    if (input.isCompetitionIdPresent()) {
      notification.setCompetition(findCompetition(input.getCompetitionId()));
    }
    if (input.isExpireAtPresent()) {
      notification.setExpireAt(input.getExpireAt());
    }
    notification.setUpdatedByUser(userReference(actingUser));

    return notificationRepository.save(notification);
  }

  /** Hard delete. */
  public void deleteNotification(Long id) {
    notificationRepository.delete(getNotification(id));
  }

  private Competition findCompetition(Long competitionId) {
    if (competitionId == null) {
      return null;
    }
    return competitionRepository
        .findById(competitionId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
  }

  private User userReference(User user) {
    return user == null ? null : userRepository.getReferenceById(user.getId());
  }

  private static String requireNotBlank(String value, String field) {
    if (value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must not be blank");
    }
    return value;
  }
}
