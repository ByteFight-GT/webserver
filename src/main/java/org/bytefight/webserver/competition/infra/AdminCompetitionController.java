package org.bytefight.webserver.competition.infra;

import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.competition.application.AdminCompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.domain.dto.AdminCompetitionDto;
import org.bytefight.webserver.competition.domain.dto.AdminCreateCompetitionDto;
import org.bytefight.webserver.competition.domain.dto.AdminUpdateCompetitionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

import jakarta.validation.Valid;
import java.util.Set;

@Tag(name = "Competition (Admin)")
@RequestMapping("/api/v1/admin/competition")
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminCompetitionController {
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "name", "slug", "isActive", "isWhitelisted");

    private final AdminCompetitionService adminCompetitionService;

    public AdminCompetitionController(AdminCompetitionService adminCompetitionService) {
        this.adminCompetitionService = adminCompetitionService;
    }

    @GetMapping
    @Operation(
            operationId = "adminListCompetitions",
            summary = "REST endpoint to list all competitions"
    )
    public Page<AdminCompetitionDto> listAll(@ModelAttribute RestPageRequest pageRequest) {
        Pageable pageable = pageRequest.toPageable(
                DEFAULT_PAGE_SIZE,
                MAX_PAGE_SIZE,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS
        );

        Page<Competition> competitionsPage = adminCompetitionService.listCompetitions(pageable);
        var data = competitionsPage.stream().map(AdminCompetitionDto::from).toList();
        return new PageImpl<>(data, competitionsPage.getPageable(), competitionsPage.getTotalElements());
    }

    @PostMapping
    @Operation(
            operationId = "adminCreateCompetition",
            summary = "REST endpoint to create a competition"
    )
    public ResponseEntity<AdminCompetitionDto> createCompetition(
            @Valid @RequestBody AdminCreateCompetitionDto input
    ) {
        Competition competition = adminCompetitionService.createCompetition(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminCompetitionDto.from(competition));
    }

    @PatchMapping("/{id}")
    @Operation(
            operationId = "adminUpdateCompetition",
            summary = "REST endpoint to update a competition"
    )
    public AdminCompetitionDto updateCompetition(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateCompetitionDto input
    ) {
        Competition competition = adminCompetitionService.updateCompetition(id, input);
        return AdminCompetitionDto.from(competition);
    }

    @GetMapping("/{id}")
    @Operation(
            operationId = "adminGetCompetition",
            summary = "REST endpoint to get a competition"
    )
    public AdminCompetitionDto getCompetition(@PathVariable Long id) {
        Competition competition = adminCompetitionService.getCompetition(id);
        return AdminCompetitionDto.from(competition);
    }
}
