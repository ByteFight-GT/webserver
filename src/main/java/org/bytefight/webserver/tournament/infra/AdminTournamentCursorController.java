package org.bytefight.webserver.tournament.infra;

import org.bytefight.webserver.tournament.application.TournamentService;
import org.bytefight.webserver.tournament.domain.CreateTournamentRequest;
import org.bytefight.webserver.tournament.domain.EnrollTeamsRequest;
import org.bytefight.webserver.tournament.domain.TournamentBracketDto;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import org.bytefight.webserver.tournament.domain.TournamentEntryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoints for tournament lifecycle management.
 *
 * Data flow summary:
 * - HTTP request -> controller -> TournamentService -> repositories + bracket builder/scheduler
 * - Match queueing uses GameMatchService via TournamentMatchScheduler
 * - Results come back through GameMatchResultHandler -> TournamentResultHandler
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/tournament_cursor")
public class AdminTournamentCursorController {
    private final TournamentService tournamentService;

    /**
     * Creates a tournament in DRAFT state.
     *
     * Path:
     * - request body -> TournamentService.createTournament
     * - persists Tournament in tournament_cursor table
     * - returns TournamentDto for frontend/admin confirmation
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TournamentDto> createTournament(@Valid @RequestBody CreateTournamentRequest request) {
        return ResponseEntity.ok(tournamentService.createTournament(request));
    }

    /**
     * Enrolls teams in a tournament and assigns seeds.
     *
     * Path:
     * - request body -> TournamentService.enrollTeams
     * - resolves teams (explicit list or all teams with submissions)
     * - creates TournamentEntry rows (tournament_cursor_entry)
     * - returns list of TournamentEntryDto for frontend
     */
    @PostMapping("/{uuid}/entries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TournamentEntryDto>> enrollTeams(
            @PathVariable String uuid,
            @RequestBody(required = false) EnrollTeamsRequest request
    ) {
        return ResponseEntity.ok(tournamentService.enrollTeams(uuid, request));
    }

    /**
     * Starts a tournament, builds the bracket, and queues all initial matches.
     *
     * Path:
     * - controller -> TournamentService.startTournament
     * - bracket generation via TournamentBracketBuilder
     * - TournamentMatch rows created (tournament_cursor_match)
     * - TournamentMatchScheduler queues matches using GameMatchService (MATCH_REASON.TOURNAMENT)
     */
    @PostMapping("/{uuid}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TournamentBracketDto> startTournament(@PathVariable String uuid) {
        return ResponseEntity.ok(tournamentService.startTournament(uuid));
    }
}
