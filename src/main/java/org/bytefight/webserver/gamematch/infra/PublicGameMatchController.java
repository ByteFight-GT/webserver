package org.bytefight.webserver.gamematch.infra;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchDto;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/game-match")
public class PublicGameMatchController {
    private final GameMatchService gameMatchService;
    private final CompetitionService competitionService;

    @GetMapping("/{uuid}")
    public ResponseEntity<GameMatchDto> getGameMatchByUuid(@PathVariable String uuid) {
        return ResponseEntity.ok(gameMatchService.getDTOByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @GetMapping
    public ResponseEntity<Page<GameMatchDto>> searchGameMatches(
            @RequestParam String competitionSlug,
            @RequestParam(required = false) String ladder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));
        Competition competition = competitionService.getCompetitionBySlug(competitionSlug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Page<GameMatch> gameMatches = gameMatchService.getPaginatedMatches(competition, ladder, pageRequest);

        return ResponseEntity.ok(gameMatches.map(GameMatchDto::fromEntity));
    }
}
