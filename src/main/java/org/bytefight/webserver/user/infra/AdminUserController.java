package org.bytefight.webserver.user.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.user.application.AdminUserService;
import org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerAndResumeDto;
import org.bytefight.webserver.user.domain.dto.EmailDomainCountDto;
import org.bytefight.webserver.user.domain.dto.ResumeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
  private static final String TIEBREAK_SORT_PATH = "u.id";

  /**
   * Admin list field name (as sent by react-admin) -> fully qualified JPQL path in the
   * UserRepository list queries ({@code FROM User u LEFT JOIN Player p ... LEFT JOIN u.resume r}).
   * Paths are qualified explicitly so they never depend on Spring Data's alias auto-prefixing.
   */
  private static final Map<String, String> SORT_FIELD_MAPPING =
      Map.of(
          "id", "u.id",
          "uuid", "u.uuid",
          "email", "u.email",
          "createdAt", "u.createdAt",
          "admin", "u.isAdmin",
          "isAdmin", "u.isAdmin",
          "playerUsername", "p.username",
          "resumeUuid", "r.uuid");

  /** Unknown/unsupported sort fields fall back to {@link #DEFAULT_SORT_FIELD} in RestPageable. */
  private static final Set<String> ALLOWED_SORT_FIELDS = SORT_FIELD_MAPPING.keySet();

  private final AdminUserService adminUserService;

  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @GetMapping
  @Operation(operationId = "adminListAllUsers", summary = "REST endpoint to list all users")
  public Page<AdminUserWithPlayerAndResumeDto> listAll(
      @ModelAttribute RestPageRequest pageRequest) {
    Pageable pageable =
        pageRequest.toPageable(
            DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);
    pageable = remapSort(pageable);
    Map<String, Object> filter = pageRequest.getFilter();
    List<Long> userIds = parseUserIds(filter);
    return adminUserService.listUsers(
        pageable, userIds, stringFilter(filter, "q"), stringFilter(filter, "emailDomain"));
  }

  /**
   * Translates list field names to JPQL paths, drops anything unmapped, and appends {@code u.id}
   * as a tiebreaker so pagination is stable when the primary sort key has duplicates/nulls.
   */
  private static Pageable remapSort(Pageable pageable) {
    List<Sort.Order> orders = new ArrayList<>();
    for (Sort.Order o : pageable.getSort()) {
      String path = SORT_FIELD_MAPPING.get(o.getProperty());
      if (path != null) {
        orders.add(new Sort.Order(o.getDirection(), path));
      }
    }
    if (orders.isEmpty()) {
      orders.add(Sort.Order.desc(SORT_FIELD_MAPPING.get(DEFAULT_SORT_FIELD)));
    }
    if (orders.stream().noneMatch(o -> o.getProperty().equals(TIEBREAK_SORT_PATH))) {
      orders.add(new Sort.Order(orders.get(0).getDirection(), TIEBREAK_SORT_PATH));
    }
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
  }

  @GetMapping("/email-domains")
  @Operation(
      operationId = "adminListUserEmailDomains",
      summary = "List distinct user email domains with user counts")
  public List<EmailDomainCountDto> listEmailDomains() {
    return adminUserService.listEmailDomains();
  }

  @GetMapping("/{userUuid}/resume")
  @Operation(
      operationId = "adminGetUserResume",
      summary = "Get a user's resume + a short-lived download link")
  public ResponseEntity<ResumeDto> getResume(@PathVariable UUID userUuid) {
    try {
      return ResponseEntity.ok(adminUserService.getResume(userUuid));
    } catch (NoSuchElementException e) {
      return ResponseEntity.notFound().build();
    }
  }

  private static String stringFilter(Map<String, Object> filter, String key) {
    return filter.get(key) instanceof String text ? text : null;
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
