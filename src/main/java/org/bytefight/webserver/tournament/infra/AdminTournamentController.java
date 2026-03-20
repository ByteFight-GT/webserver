package org.bytefight.webserver.tournament.infra;

import org.bytefight.webserver.tournament.application.TournamentService;
import org.bytefight.webserver.tournament.domain.CreateTournamentRequest;
import org.bytefight.webserver.tournament.domain.TournamentBracketDto;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoints for tournament lifecycle management, scoped to a competition.
 *
 * Data flow summary:
 * - HTTP request -> controller -> TournamentService -> repositories + bracket builder/scheduler
 * - Match queueing uses GameMatchService via TournamentMatchScheduler
 * - Results come back through GameMatchResultHandler -> TournamentResultHandler
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/competition/{competitionSlug}/tournament")
public class AdminTournamentController {
    private final TournamentService tournamentService;

    /**
     * Returns all tournaments for a competition (newest first).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TournamentDto>> getTournaments(
            @PathVariable String competitionSlug
    ) {
        return ResponseEntity.ok(tournamentService.getTournaments(competitionSlug));
    }

    /**
     * Creates a tournament and enrolls teams for the given competition.
     *
     * Path:
     * - request body -> TournamentService.createTournament
     * - persists Tournament in tournament table
     * - resolves and seeds tournament entries in tournament_entry table
     * - updates tournament status to OPEN
     * - returns TournamentDto for frontend/admin confirmation
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TournamentDto> createTournament(
            @PathVariable String competitionSlug,
            @Valid @RequestBody CreateTournamentRequest request
    ) {
        return ResponseEntity.ok(tournamentService.createTournament(competitionSlug, request));
    }

    /**
     * Starts a tournament, builds the bracket, and queues all initial matches.
     *
     * Path:
     * - controller -> TournamentService.startTournament
     * - bracket generation via TournamentBracketBuilder
     * - TournamentMatch rows created (tournament_match)
     * - TournamentMatchScheduler queues matches using GameMatchService (MatchReason.tournament)
     */
    @PostMapping("/{uuid}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TournamentBracketDto> startTournament(
            @PathVariable String competitionSlug,
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(tournamentService.startTournament(competitionSlug, uuid));
    }
}
