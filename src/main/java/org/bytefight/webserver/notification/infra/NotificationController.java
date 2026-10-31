package org.bytefight.webserver.notification.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.notification.application.NotificationService;
import org.bytefight.webserver.notification.domain.dto.NotificationDto;
import org.bytefight.webserver.notification.domain.dto.UnreadCountDto;
import org.bytefight.webserver.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification")
@RequestMapping("/api/v1/notification")
@RestController
@RequiredArgsConstructor
public class NotificationController {
  private static final int MAX_PAGE_SIZE = 50;

  private final NotificationService notificationService;

  @GetMapping
  @Operation(
      operationId = "getNotificationFeed",
      summary =
          "Site-wide notifications plus those for the user's competitions, unexpired, newest first")
  public ResponseEntity<Page<NotificationDto>> getFeed(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    int resolvedPage = Math.max(page, 0);
    int resolvedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    return ResponseEntity.ok(notificationService.getFeed(user, resolvedPage, resolvedSize));
  }

  @GetMapping("/unread-count")
  @Operation(
      operationId = "getNotificationUnreadCount",
      summary = "Number of notifications created since the user last called mark-read")
  public ResponseEntity<UnreadCountDto> getUnreadCount(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(new UnreadCountDto(notificationService.getUnreadCount(user)));
  }

  @PostMapping("/mark-read")
  @Operation(operationId = "markNotificationsRead", summary = "Mark all notifications as read")
  public ResponseEntity<Void> markRead(@AuthenticationPrincipal User user) {
    notificationService.markRead(user);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/urgent")
  @Operation(
      operationId = "getUrgentNotifications",
      summary = "Active urgent notifications for the user, for the site banner")
  public ResponseEntity<List<NotificationDto>> getUrgent(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(notificationService.getActiveUrgent(user));
  }
}
