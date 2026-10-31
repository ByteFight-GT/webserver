package org.bytefight.webserver.notification.application;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bytefight.webserver.notification.domain.Notification;
import org.bytefight.webserver.notification.domain.NotificationPriority;
import org.bytefight.webserver.notification.domain.dto.NotificationDto;
import org.bytefight.webserver.notification.infra.NotificationRepository;
import org.bytefight.webserver.notification.infra.NotificationSpecifications;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The user-facing side of notifications. A user sees site-wide notifications plus those for every
 * competition they are on a (non-deleted) team in; internal competitions follow the same rule.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

  private final NotificationRepository notificationRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final PlayerService playerService;
  private final UserRepository userRepository;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Page<NotificationDto> getFeed(User user, int page, int size) {
    Instant lastChecked = user.getLastNotificationsChecked();
    Pageable pageable = PageRequest.of(page, size, NEWEST_FIRST);
    return notificationRepository
        .findAll(visibleTo(user), pageable)
        .map(notification -> NotificationDto.from(notification, lastChecked));
  }

  @Transactional(readOnly = true)
  public long getUnreadCount(User user) {
    return notificationRepository.count(
        visibleTo(user)
            .and(NotificationSpecifications.createdAfter(user.getLastNotificationsChecked())));
  }

  @Transactional
  public void markRead(User user) {
    User managed = userRepository.findById(user.getId()).orElseThrow();
    managed.setLastNotificationsChecked(clock.instant());
    userRepository.save(managed);
  }

  @Transactional(readOnly = true)
  public List<NotificationDto> getActiveUrgent(User user) {
    Instant lastChecked = user.getLastNotificationsChecked();
    return notificationRepository
        .findAll(
            visibleTo(user)
                .and(NotificationSpecifications.hasPriority(NotificationPriority.URGENT)),
            NEWEST_FIRST)
        .stream()
        .map(notification -> NotificationDto.from(notification, lastChecked))
        .toList();
  }

  /** Site-wide urgent notifications for logged-out visitors. {@code unread} is always false. */
  @Transactional(readOnly = true)
  public List<NotificationDto> getPublicUrgent() {
    return notificationRepository
        .findAll(
            NotificationSpecifications.visibleTo(Set.of(), clock.instant())
                .and(NotificationSpecifications.hasPriority(NotificationPriority.URGENT)),
            NEWEST_FIRST)
        .stream()
        .map(notification -> NotificationDto.from(notification, null))
        .toList();
  }

  private Specification<Notification> visibleTo(User user) {
    return NotificationSpecifications.visibleTo(competitionIds(user), clock.instant());
  }

  private Set<Long> competitionIds(User user) {
    return playerService
        .getPlayer(user)
        .map(
            player ->
                teamMemberRepository.findByPlayerAndTeamDeletedAtNull(player).stream()
                    .map(member -> member.getCompetition().getId())
                    .collect(Collectors.toSet()))
        .orElse(Set.of());
  }
}
