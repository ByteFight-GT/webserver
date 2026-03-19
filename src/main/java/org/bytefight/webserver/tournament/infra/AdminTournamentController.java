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
     * Creates a tournament in DRAFT state for the given competition.
     *
     * Path:
     * - request body -> TournamentService.createTournament
     * - persists Tournament in tournament table
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
     * Enrolls teams in a tournament and assigns seeds within the competition.
     *
     * Path:
     * - request body -> TournamentService.enrollTeams
     * - resolves teams (explicit list or all teams with submissions)
     * - creates TournamentEntry rows (tournament_entry)
     * - returns list of TournamentEntryDto for frontend
     */
    @PostMapping("/{uuid}/entries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TournamentEntryDto>> enrollTeams(
            @PathVariable String competitionSlug,
            @PathVariable String uuid,
            @RequestBody(required = false) EnrollTeamsRequest request
    ) {
        return ResponseEntity.ok(tournamentService.enrollTeams(competitionSlug, uuid, request));
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
