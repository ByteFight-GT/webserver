package org.bytefight.webserver.notification.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.notification.application.NotificationService;
import org.bytefight.webserver.notification.domain.dto.NotificationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification (Public)")
@RequestMapping("/api/v1/public/notification")
@RestController
@RequiredArgsConstructor
public class PublicNotificationController {
  private final NotificationService notificationService;

  @GetMapping("/urgent")
  @Operation(
      operationId = "getPublicUrgentNotifications",
      summary =
          "Active site-wide urgent notifications, for the banner shown to logged-out visitors")
  public ResponseEntity<List<NotificationDto>> getUrgent() {
    return ResponseEntity.ok(notificationService.getPublicUrgent());
  }
}
