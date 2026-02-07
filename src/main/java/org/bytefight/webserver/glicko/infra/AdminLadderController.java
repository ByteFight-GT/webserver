package org.bytefight.webserver.glicko.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.glicko.application.AdminLadderService;
import org.bytefight.webserver.glicko.domain.Ladder;
import org.bytefight.webserver.glicko.domain.dto.AdminCreateLadderDto;
import org.bytefight.webserver.glicko.domain.dto.AdminLadderDto;
import org.bytefight.webserver.glicko.domain.dto.AdminUpdateLadderDto;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import java.util.Set;

@Tag(name = "Ladder (Admin)")
@RequestMapping({"/api/v1/admin/ladder"})
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminLadderController {
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "ladder";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "ladder",
            "glickoDefaultRating",
            "glickoDefaultRd",
            "glickoRdMax",
            "glickoRdMin",
            "glickoPhiInflationPerDay",
            "glickoTau",
            "glickoSigmaDefault",
            "glickoSigmaMin",
            "glickoSigmaMax"
    );

    private final AdminLadderService adminLadderService;

    public AdminLadderController(AdminLadderService adminLadderService) {
        this.adminLadderService = adminLadderService;
    }

    @GetMapping
    @Operation(
            operationId = "adminListLadders",
            summary = "REST endpoint to list ladders by competition"
    )
    public Page<AdminLadderDto> listLadders(
            @ModelAttribute RestPageRequest pageRequest
    ) {
        Specification<Ladder> specification = LadderSpecifications.fromFilter(pageRequest.getFilter());
        Pageable pageable = pageRequest.toPageable(
                DEFAULT_PAGE_SIZE,
                MAX_PAGE_SIZE,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS
        );
        Page<Ladder> ladders = adminLadderService.listLadders(specification, pageable);
        var data = ladders.stream().map(AdminLadderDto::from).toList();
        return new PageImpl<>(data, ladders.getPageable(), ladders.getTotalElements());
    }

    @PostMapping
    @Operation(
            operationId = "adminCreateLadder",
            summary = "REST endpoint to create a ladder"
    )
    public ResponseEntity<AdminLadderDto> createLadder(
            @Valid @RequestBody AdminCreateLadderDto input
    ) {
        Ladder ladder = adminLadderService.createLadder(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminLadderDto.from(ladder));
    }

    @PatchMapping("/{id}")
    @Operation(
            operationId = "adminUpdateLadder",
            summary = "REST endpoint to update a ladder"
    )
    public AdminLadderDto updateLadder(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateLadderDto input
    ) {
        Ladder ladder = adminLadderService.updateLadder(id, input);
        return AdminLadderDto.from(ladder);
    }

    @GetMapping("/{id}")
    @Operation(
            operationId = "adminGetLadder",
            summary = "REST endpoint to get a ladder"
    )
    public AdminLadderDto getLadder(@PathVariable Long id) {
        Ladder ladder = adminLadderService.getLadder(id);
        return AdminLadderDto.from(ladder);
    }

}
