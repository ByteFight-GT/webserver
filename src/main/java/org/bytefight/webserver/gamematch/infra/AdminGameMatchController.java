package org.bytefight.webserver.gamematch.infra;

import org.bytefight.webserver.gamematch.application.AdminGameMatchService;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.AdminCreateMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.AdminGameMatchDto;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/game-match")
public class AdminGameMatchController {
    private final AdminGameMatchService adminGameMatchService;
    private final GameMatchService gameMatchService;

    @PostMapping("/reschedule-stale")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reschduleStaleMatches() {
        gameMatchService.rescheduleStaleMatches(false);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reschedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminRescheduleMatches(@RequestBody List<Long> matchIds) {
        gameMatchService.adminRescheduleMatches(matchIds);

        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<AdminGameMatchDto> adminCreateGameMatch(
            @Valid @RequestBody AdminCreateMatchDto input
    ) {
        GameMatch match = adminGameMatchService.createMatch(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminGameMatchDto.fromEntity(match));
    }
}
