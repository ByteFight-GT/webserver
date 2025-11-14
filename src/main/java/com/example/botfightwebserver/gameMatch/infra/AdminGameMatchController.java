package com.example.botfightwebserver.gameMatch.infra;

import com.example.botfightwebserver.gameMatch.application.AdminGameMatchService;
import com.example.botfightwebserver.gameMatch.domain.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/game-match")
public class AdminGameMatchController {
    private final AdminGameMatchService adminGameMatchService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminGameMatchDto> adminListGameMatches(
            Pageable pageable,
            @RequestParam(required = false) List<MATCH_STATUS> status,
            @RequestParam(required = false) List<MATCH_REASON> reason
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
