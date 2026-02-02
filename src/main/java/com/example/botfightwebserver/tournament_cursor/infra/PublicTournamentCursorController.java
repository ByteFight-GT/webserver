package com.example.botfightwebserver.tournament_cursor.infra;

import com.example.botfightwebserver.tournament_cursor.application.TournamentService;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentBracketDto;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentDto;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentMatchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public read-only endpoints for tournament visualization.
 *
 * Data flow summary:
 * - HTTP request -> controller -> TournamentService -> repositories
 * - Returns DTOs designed for frontend bracket rendering
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/tournament_cursor")
public class PublicTournamentCursorController {
    private final TournamentService tournamentService;

    /**
     * Returns tournament metadata (status, sizes, timestamps).
     *
     * Path:
     * - controller -> TournamentService.getTournamentDto
     * - reads Tournament by uuid (tournament_cursor table)
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<TournamentDto> getTournament(@PathVariable String uuid) {
        return ResponseEntity.ok(tournamentService.getTournamentDto(uuid));
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
    public ResponseEntity<TournamentBracketDto> getBracket(@PathVariable String uuid) {
        return ResponseEntity.ok(tournamentService.getBracket(uuid));
    }

    /**
     * Returns only tournament matches, ordered for timeline/bracket views.
     *
     * Path:
     * - controller -> TournamentService.getMatches
     * - reads TournamentMatch rows and maps to TournamentMatchDto
     */
    @GetMapping("/{uuid}/matches")
    public ResponseEntity<List<TournamentMatchDto>> getMatches(@PathVariable String uuid) {
        return ResponseEntity.ok(tournamentService.getMatches(uuid));
    }
}
