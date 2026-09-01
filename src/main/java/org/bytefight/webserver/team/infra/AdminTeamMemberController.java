package org.bytefight.webserver.team.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.domain.dto.AdminPlayerDto;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.team.application.AdminTeamService;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Team Member (Admin)")
@RequestMapping("/api/v1/admin/team-member")
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminTeamMemberController {
  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 100;
  private static final String DEFAULT_SORT_FIELD = "id";
  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "username", "createdAt");

  private final AdminTeamService adminTeamService;
  private final PlayerRepository playerRepository;

  public AdminTeamMemberController(
      AdminTeamService adminTeamService, PlayerRepository playerRepository) {
    this.adminTeamService = adminTeamService;
    this.playerRepository = playerRepository;
  }

  @GetMapping
  @Operation(operationId = "adminListTeamMembers", summary = "REST endpoint to list team members")
  public Page<AdminPlayerDto> listMembers(@ModelAttribute RestPageRequest pageRequest) {
    Pageable pageable =
        pageRequest.toPageable(
            DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

    Long teamId = parseTeamId(pageRequest.getFilter());
    if (teamId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId is required");
    }

    Team team = adminTeamService.getTeam(teamId);
    Page<Player> membersPage = playerRepository.findByTeamId(teamId, pageable);
    List<AdminPlayerDto> data = membersPage.stream().map(AdminPlayerDto::from).toList();
    return new PageImpl<>(data, membersPage.getPageable(), membersPage.getTotalElements());
  }

  private static Long parseTeamId(Map<String, Object> filter) {
    if (filter == null || filter.isEmpty()) {
      return null;
    }
    Object value = filter.get("teamId");
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
