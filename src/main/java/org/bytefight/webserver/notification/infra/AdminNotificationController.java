package org.bytefight.webserver.notification.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.notification.application.AdminNotificationService;
import org.bytefight.webserver.notification.domain.Notification;
import org.bytefight.webserver.notification.domain.dto.AdminCreateNotificationDto;
import org.bytefight.webserver.notification.domain.dto.AdminNotificationDto;
import org.bytefight.webserver.notification.domain.dto.AdminUpdateNotificationDto;
import org.bytefight.webserver.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification (Admin)")
@RequestMapping("/api/v1/admin/notification")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
public class AdminNotificationController {
  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 100;
  private static final String DEFAULT_SORT_FIELD = "createdAt";
  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("id", "createdAt", "updatedAt", "expireAt", "priority", "title");

  private final AdminNotificationService adminNotificationService;

  @GetMapping
  @Operation(
      operationId = "adminListNotifications",
      summary = "REST endpoint to list notifications, newest first by default")
  public Page<AdminNotificationDto> listNotifications(@ModelAttribute RestPageRequest pageRequest) {
    Specification<Notification> specification =
        NotificationSpecifications.fromFilter(pageRequest.getFilter());
    Pageable pageable =
        pageRequest.toPageable(
            DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);
    Page<Notification> notifications =
        adminNotificationService.listNotifications(specification, pageable);
    var data = notifications.stream().map(AdminNotificationDto::from).toList();
    return new PageImpl<>(data, notifications.getPageable(), notifications.getTotalElements());
  }

  @GetMapping("/{id}")
  @Operation(operationId = "adminGetNotification", summary = "REST endpoint to get a notification")
  public AdminNotificationDto getNotification(@PathVariable Long id) {
    return AdminNotificationDto.from(adminNotificationService.getNotification(id));
  }

  @PostMapping
  @Operation(
      operationId = "adminCreateNotification",
      summary = "REST endpoint to create a notification")
  public ResponseEntity<AdminNotificationDto> createNotification(
      @AuthenticationPrincipal User user, @Valid @RequestBody AdminCreateNotificationDto input) {
    Notification notification = adminNotificationService.createNotification(input, user);
    return ResponseEntity.status(HttpStatus.CREATED).body(AdminNotificationDto.from(notification));
  }

  @PatchMapping("/{id}")
  @Operation(
      operationId = "adminUpdateNotification",
      summary = "REST endpoint to partially update a notification")
  public AdminNotificationDto updateNotification(
      @AuthenticationPrincipal User user,
      @PathVariable Long id,
      @Valid @RequestBody AdminUpdateNotificationDto input) {
    return AdminNotificationDto.from(adminNotificationService.updateNotification(id, input, user));
  }

  /** Returns the deleted record because ra-data-simple-rest reads the DELETE response body. */
  @DeleteMapping("/{id}")
  @Operation(
      operationId = "adminDeleteNotification",
      summary = "REST endpoint to delete a notification")
  public AdminNotificationDto deleteNotification(@PathVariable Long id) {
    AdminNotificationDto deleted =
        AdminNotificationDto.from(adminNotificationService.getNotification(id));
    adminNotificationService.deleteNotification(id);
    return deleted;
  }
}
