package org.bytefight.webserver.competition.infra;

import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.competition.application.AdminCompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.domain.dto.AdminCompetitionDto;
import org.bytefight.webserver.competition.domain.dto.AdminCreateCompetitionDto;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Set;

@Tag(name = "Competition (Admin)")
@RestController
@RequestMapping("/api/v1/admin/competition")
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
        return competitionsPage.map(AdminCompetitionDto::from);
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
}
