package org.bytefight.webserver.gamematch.infra;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.gamematch.application.AdminGameMatchService;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.dto.AdminCreateMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.AdminGameMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.AdminRescheduleResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Game Match (Admin)")
@RequestMapping("/api/v1/admin/game-match")
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminGameMatchController {
  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 500;
  private static final String DEFAULT_SORT_FIELD = "createdAt";
  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("createdAt", "scheduledAt", "startedAt", "finishedAt", "status", "reason", "id");

  private final AdminGameMatchService adminGameMatchService;
  private final GameMatchService gameMatchService;

  public AdminGameMatchController(
      AdminGameMatchService adminGameMatchService, GameMatchService gameMatchService) {
    this.adminGameMatchService = adminGameMatchService;
    this.gameMatchService = gameMatchService;
  }

  @PostMapping("/schedule")
  public ResponseEntity<Void> adminScheduleMatches(@RequestBody List<String> matchUuids) {
    for (String uuid : matchUuids) {
      GameMatch gameMatch = gameMatchService.getGameMatch(UUID.fromString(uuid)).orElse(null);
      if (gameMatch == null) continue;

      gameMatchService.scheduleMatch(gameMatch);
    }

    return ResponseEntity.ok().build();
  }

  @PostMapping("/reschedule")
  public AdminRescheduleResultDto adminRescheduleMatches(@RequestBody List<Long> matchIds) {
    if (matchIds == null || matchIds.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No match ids provided");
    }
    return adminGameMatchService.rescheduleMatches(matchIds);
  }

  @GetMapping
  public Page<AdminGameMatchDto> adminListGameMatches(@ModelAttribute RestPageRequest pageRequest) {
    Pageable pageable =
        pageRequest.toPageable(
            DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);
    Specification<GameMatch> specs = GameMatchSpecifications.fromFilter(pageRequest.getFilter());
    return adminGameMatchService.list(specs, pageable);
  }

  @PostMapping("/{id}/fail")
  public ResponseEntity<Void> adminFailGameMatch(@PathVariable Long id) {
    adminGameMatchService.markFailed(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  public AdminGameMatchDto adminGetGameMatch(@PathVariable Long id) {
    return adminGameMatchService.get(id);
  }

  @PostMapping
  public ResponseEntity<AdminGameMatchDto> adminCreateGameMatch(
      @Valid @RequestBody AdminCreateMatchDto input) {
    GameMatch match = adminGameMatchService.createMatch(input);
    return ResponseEntity.status(HttpStatus.CREATED).body(AdminGameMatchDto.fromEntity(match));
  }
}
