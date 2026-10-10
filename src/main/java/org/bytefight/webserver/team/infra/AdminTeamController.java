package org.bytefight.webserver.team.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.team.application.AdminTeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.domain.TeamMemberDetails;
import org.bytefight.webserver.team.domain.TeamType;
import org.bytefight.webserver.team.domain.dto.AdminCreateTeamDto;
import org.bytefight.webserver.team.domain.dto.AdminTeamDto;
import org.bytefight.webserver.team.domain.dto.AdminTeamWithMemberDto;
import org.bytefight.webserver.team.domain.dto.AdminUpdateTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Team (Admin)")
@RequestMapping("/api/v1/admin/team")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
public class AdminTeamController {
  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 100;
  private static final String DEFAULT_SORT_FIELD = "createdAt";
  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("createdAt", "id", "name", "uuid", "isDeleted", "type");

  private final AdminTeamService adminTeamService;
  private final PlayerRepository playerRepository;

  @GetMapping
  @Operation(operationId = "adminListTeams", summary = "REST endpoint to list all teams")
  public Page<AdminTeamWithMemberDto> listAll(@ModelAttribute RestPageRequest pageRequest) {
    Pageable pageable =
        pageRequest.toPageable(
            DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

    Page<Team> teams =
        adminTeamService.listTeams(buildTeamSpecification(pageRequest.getFilter()), pageable);
    List<Team> teamList = teams.getContent();
    List<Long> pagedTeamIds = teamList.stream().map(Team::getId).toList();

    Map<Long, List<AdminTeamWithMemberDto.MemberDto>> membersByTeamId = new HashMap<>();
    if (!pagedTeamIds.isEmpty()) {
      List<TeamMemberDetails> memberDetails =
          playerRepository.findMemberDetailsByTeamIds(pagedTeamIds);
      for (TeamMemberDetails member : memberDetails) {
        membersByTeamId
            .computeIfAbsent(member.getTeamId(), ignored -> new ArrayList<>())
            .add(AdminTeamWithMemberDto.MemberDto.from(member));
      }
    }

    List<AdminTeamWithMemberDto> data =
        teamList.stream()
            .map(
                team ->
                    AdminTeamWithMemberDto.from(
                        team, membersByTeamId.getOrDefault(team.getId(), List.of())))
            .toList();
    return new PageImpl<>(data, teams.getPageable(), teams.getTotalElements());
  }

  @PostMapping
  @Operation(operationId = "adminCreateTeam", summary = "REST endpoint to create a team")
  public ResponseEntity<AdminTeamDto> createTeam(@Valid @RequestBody AdminCreateTeamDto input) {
    Team team = adminTeamService.createTeam(input);
    return ResponseEntity.status(HttpStatus.CREATED).body(AdminTeamDto.from(team));
  }

  @PatchMapping("/{id}")
  @Operation(operationId = "adminUpdateTeam", summary = "REST endpoint to update a team")
  public AdminTeamDto updateTeam(
      @PathVariable Long id, @Valid @RequestBody AdminUpdateTeamDto input) {
    Team team = adminTeamService.updateTeam(id, input);
    return AdminTeamDto.from(team);
  }

  @GetMapping("/{id}")
  @Operation(operationId = "adminGetTeam", summary = "REST endpoint to get a team")
  public AdminTeamDto getTeam(@PathVariable Long id) {
    Team team = adminTeamService.getTeam(id);
    return AdminTeamDto.from(team);
  }

  private static Specification<Team> buildTeamSpecification(Map<String, Object> filter) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      Map<String, Object> safeFilter = filter != null ? filter : Map.of();

      List<Long> ids = parseLongList(safeFilter.get("id"));
      if (!ids.isEmpty()) {
        // Lookups by id (e.g. reference fields) return teams regardless of deletion state.
        predicates.add(root.get("id").in(ids));
      } else {
        Boolean isDeleted = parseBoolean(safeFilter.get("isDeleted"));
        predicates.add(cb.equal(root.get("isDeleted"), isDeleted != null ? isDeleted : false));
      }

      Long competitionId = parseLong(safeFilter.get("competitionId"));
      if (competitionId != null) {
        predicates.add(cb.equal(root.get("competition").get("id"), competitionId));
      }

      TeamType type = parseType(safeFilter.get("type"));
      if (type != null) {
        predicates.add(cb.equal(root.get("type"), type));
      }

      String name = normalize(safeFilter.get("name"));
      if (name != null) {
        predicates.add(cb.like(root.get("nameNormalized"), containsPattern(name), '\\'));
      }

      String playerUsername = normalize(safeFilter.get("playerUsername"));
      if (playerUsername != null) {
        Subquery<Long> members = query.subquery(Long.class);
        Root<TeamMember> member = members.from(TeamMember.class);
        members
            .select(member.get("team").get("id"))
            .where(
                cb.like(
                    member.get("player").get("usernameNormalized"),
                    containsPattern(playerUsername),
                    '\\'));
        predicates.add(root.get("id").in(members));
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String containsPattern(String text) {
    String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped + "%";
  }

  private static String normalize(Object value) {
    if (value instanceof String text && !text.isBlank()) {
      return text.trim().toLowerCase(Locale.ROOT);
    }
    return null;
  }

  private static TeamType parseType(Object value) {
    if (value instanceof String text && !text.isBlank()) {
      try {
        return TeamType.valueOf(text.trim());
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
    return null;
  }

  private static Boolean parseBoolean(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof String text && !text.isBlank()) {
      return Boolean.parseBoolean(text);
    }
    return null;
  }

  private static List<Long> parseLongList(Object value) {
    if (value == null) {
      return List.of();
    }
    List<Long> ids = new ArrayList<>();
    if (value instanceof Collection<?> values) {
      for (Object item : values) {
        Long parsed = parseLong(item);
        if (parsed != null) {
          ids.add(parsed);
        }
      }
      return ids;
    }
    if (value instanceof String text) {
      for (String part : text.split(",")) {
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
