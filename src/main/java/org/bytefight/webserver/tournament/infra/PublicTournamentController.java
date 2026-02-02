package org.bytefight.webserver.tournament.infra;

import org.bytefight.webserver.tournament.application.TournamentService;
import org.bytefight.webserver.tournament.domain.TournamentBracketDto;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import org.bytefight.webserver.tournament.domain.TournamentMatchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public read-only endpoints for tournament visualization, scoped to a competition.
 *
 * Data flow summary:
 * - HTTP request -> controller -> TournamentService -> repositories
 * - Returns DTOs designed for frontend bracket rendering
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/competition/{competitionSlug}/tournament")
public class PublicTournamentController {
    private final TournamentService tournamentService;

    /**
     * Returns tournament metadata (status, sizes, timestamps).
     *
     * Path:
     * - controller -> TournamentService.getTournamentDto
     * - reads Tournament by uuid (tournament table)
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<TournamentDto> getTournament(
            @PathVariable String competitionSlug,
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(tournamentService.getTournamentDto(competitionSlug, uuid));
    }

    /**
     * Returns full bracket view: tournament + entries + matches.
     *
     * Path:
     * - controller -> TournamentService.getBracket
     * - reads Tournament, TournamentEntry, TournamentMatch rows
     * - maps to DTOs for frontend to render bracket graph
     */
    @GetMapping("/{uuid}/bracket")
    public ResponseEntity<TournamentBracketDto> getBracket(
            @PathVariable String competitionSlug,
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(tournamentService.getBracket(competitionSlug, uuid));
    }

    /**
     * Returns only tournament matches, ordered for timeline/bracket views.
     *
     * Path:
     * - controller -> TournamentService.getMatches
     * - reads TournamentMatch rows and maps to TournamentMatchDto
     */
    @GetMapping("/{uuid}/matches")
    public ResponseEntity<List<TournamentMatchDto>> getMatches(
            @PathVariable String competitionSlug,
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(tournamentService.getMatches(competitionSlug, uuid));
    }
}
