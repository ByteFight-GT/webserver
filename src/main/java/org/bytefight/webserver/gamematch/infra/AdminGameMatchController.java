package org.bytefight.webserver.gamematch.infra;

import org.bytefight.webserver.gamematch.application.AdminGameMatchService;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.AdminGameMatchDto;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminGameMatchDto> adminListGameMatches(
            Pageable pageable,
            @RequestParam(required = false) List<MatchStatus> status,
            @RequestParam(required = false) List<MatchReason> reason
    ) {
        Specification<GameMatch> specs = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(status != null) {
                predicates.add(root.get("status").in(status));
            }

            if(reason != null) {
                predicates.add(root.get("reason").in(reason));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return adminGameMatchService.list(specs, pageable);
    }
}
