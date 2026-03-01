package org.bytefight.webserver.auth.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bytefight.webserver.auth.application.AdminUserService;
import org.bytefight.webserver.auth.domain.dto.AdminUserWithPlayerDto;
import org.bytefight.webserver.common.web.RestPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User (Admin)")
@RequestMapping("/api/v1/admin/user")
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminUserController {
  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 100;
  private static final String DEFAULT_SORT_FIELD = "createdAt";
  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("createdAt", "email", "isAdmin", "uuid");

  private final AdminUserService adminUserService;

  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @GetMapping
  @Operation(operationId = "adminListAllUsers", summary = "REST endpoint to list all users")
  public Page<AdminUserWithPlayerDto> listAll(@ModelAttribute RestPageRequest pageRequest) {
    Pageable pageable =
        pageRequest.toPageable(
            DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);
    List<Long> userIds = parseUserIds(pageRequest.getFilter());
    return adminUserService.listUsers(pageable, userIds);
  }

  private static List<Long> parseUserIds(Map<String, Object> filter) {
    if (filter == null || filter.isEmpty()) {
      return List.of();
    }
    Object value = filter.get("id");
    if (value == null) {
      return List.of();
    }
    if (value instanceof Collection<?> values) {
      List<Long> ids = new ArrayList<>();
      for (Object item : values) {
        Long parsed = parseLong(item);
        if (parsed != null) {
          ids.add(parsed);
        }
      }
      return ids;
    }
    if (value instanceof String text && !text.isBlank()) {
      String[] parts = text.split(",");
      List<Long> ids = new ArrayList<>();
      for (String part : parts) {
        Long parsed = parseLong(part);
        if (parsed != null) {
          ids.add(parsed);
        }
      }
      return ids;
    }
    Long single = parseLong(value);
    return single != null ? List.of(single) : List.of();
  }

  private static Long parseLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return Long.parseLong(text.trim());
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return null;
  }
}
