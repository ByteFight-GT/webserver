package org.bytefight.webserver.team.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.team.application.AdminTeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.dto.AdminCreateTeamDto;
import org.bytefight.webserver.team.domain.dto.AdminTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;

@Tag(name = "Team (Admin)")
@RestController
@RequestMapping("/api/v1/admin/team")
public class AdminTeamController {
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "id", "name", "uuid", "isDeleted", "type");

    private final AdminTeamService adminTeamService;

    public AdminTeamController(AdminTeamService adminTeamService) {
        this.adminTeamService = adminTeamService;
    }

    @GetMapping
    @Operation(
            operationId = "adminListTeams",
            summary = "REST endpoint to list all teams"
    )
    public Page<AdminTeamDto> listAll(@ModelAttribute RestPageRequest pageRequest) {
        Pageable pageable = pageRequest.toPageable(
                DEFAULT_PAGE_SIZE,
                MAX_PAGE_SIZE,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS
        );

        Map<String, Object> filter = pageRequest.getFilter();
        Long competitionId = parseCompetitionId(filter);
        Boolean isDeleted = parseIsDeleted(filter);
        boolean resolvedIsDeleted = isDeleted != null ? isDeleted : false;

        Page<Team> teams = adminTeamService.listTeams(competitionId, resolvedIsDeleted, pageable);
        var data = teams.stream().map(AdminTeamDto::from).toList();
        return new PageImpl<>(data, teams.getPageable(), teams.getTotalElements());
    }

    @PostMapping
    @Operation(
            operationId = "adminCreateTeam",
            summary = "REST endpoint to create a team"
    )
    public ResponseEntity<AdminTeamDto> createTeam(
            @Valid @RequestBody AdminCreateTeamDto input
    ) {
        Team team = adminTeamService.createTeam(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminTeamDto.from(team));
    }

    private static Long parseCompetitionId(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        Object value = filter.get("competitionId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static Boolean parseIsDeleted(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        Object value = filter.get("isDeleted");
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }
}
