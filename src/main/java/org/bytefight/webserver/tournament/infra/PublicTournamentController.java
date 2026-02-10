package org.bytefight.webserver.tournament.infra;

import org.bytefight.webserver.tournament.application.TournamentService;
import org.bytefight.webserver.tournament.domain.TournamentBracketDto;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import org.bytefight.webserver.tournament.domain.TournamentEntryDto;
import org.bytefight.webserver.tournament.domain.TournamentMatchDto;
import org.bytefight.webserver.tournament.domain.TournamentRankingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public read-only endpoints for tournament visualization, scoped to a competition.
 *
 * Endpoint summary:
 *   GET /{uuid}            -> tournament metadata (status, sizes, timestamps, 1st/2nd place)
 *   GET /{uuid}/teams      -> all enrolled teams (sorted by seed)
 *   GET /{uuid}/rounds     -> all matches/series across every bracket type and round
 *   GET /{uuid}/rounds?bracketType=WINNERS&fromRound=1&toRound=3
 *                          -> matches filtered by bracket type and/or round range
 *   GET /{uuid}/all        -> everything combined (tournament + teams + matches)
 *   GET /{uuid}/rankings   -> final placement of all teams (only when tournament is COMPLETE)
 *
 * Data flow:
 *   HTTP request -> controller -> TournamentService -> repositories -> DTOs
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/competition/{competitionSlug}/tournament")
public class PublicTournamentController {
    private final TournamentService tournamentService;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Tournament metadata
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns tournament metadata: status, sizes, timestamps, and 1st/2nd place
     * (if the tournament is complete).
     *
     * Path:
     *   controller -> TournamentService.getTournamentDto
     *   -> reads Tournament row by UUID (tournament table)
     *   -> maps to TournamentDto
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<TournamentDto> getTournament(
            @PathVariable String competitionSlug,
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(tournamentService.getTournamentDto(competitionSlug, uuid));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. All teams data
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all enrolled teams (tournament entries) sorted by seed.
     * Each entry includes team UUID, name, seed, loss count, and elimination status.
     *
     * Path:
     *   controller -> TournamentService.getTeams
     *   -> reads TournamentEntry rows ordered by seed (tournament_entry table)
     *   -> maps to List<TournamentEntryDto>
     */
    @GetMapping("/{uuid}/teams")
    public ResponseEntity<List<TournamentEntryDto>> getTeams(
            @PathVariable String competitionSlug,
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(tournamentService.getTeams(competitionSlug, uuid));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Rounds (all or filtered range)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns matches (series) with optional filtering by bracket type and round range.
     *
     * Query parameters (all optional):
     *   bracketType  -> WINNERS, LOSERS, GRAND_FINAL, or GRAND_FINAL_RESET
     *   fromRound    -> inclusive lower bound on round number
     *   toRound      -> inclusive upper bound on round number
     *
     * Examples:
     *   GET /rounds                                  -> all matches across all brackets
     *   GET /rounds?bracketType=WINNERS              -> all winners bracket matches
     *   GET /rounds?bracketType=LOSERS&fromRound=1&toRound=4
     *                                                -> losers bracket rounds 1-4
     *   GET /rounds?fromRound=2&toRound=2            -> round 2 across all bracket types
     *
     * Path:
     *   controller -> TournamentService.getMatchesByRound
     *   -> reads all TournamentMatch rows for the tournament (tournament_match table)
     *   -> filters in-memory by bracketType / fromRound / toRound
     *   -> maps to List<TournamentMatchDto> (each includes series scores and individual games)
     */
    @GetMapping("/{uuid}/rounds")
    public ResponseEntity<List<TournamentMatchDto>> getRounds(
            @PathVariable String competitionSlug,
            @PathVariable String uuid,
            @RequestParam(required = false) TournamentBracketType bracketType,
            @RequestParam(required = false) Integer fromRound,
            @RequestParam(required = false) Integer toRound
    ) {
        return ResponseEntity.ok(
                tournamentService.getMatchesByRound(competitionSlug, uuid, bracketType, fromRound, toRound)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. All data (tournament + teams + matches)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full tournament payload: metadata, all enrolled teams, and all
     * matches with their series data and individual game results.
     *
     * This is the "everything" endpoint — ideal for initial page load when the
     * frontend needs all bracket data in a single request.
     *
     * Path:
     *   controller -> TournamentService.getBracket
     *   -> reads Tournament, TournamentEntry, and TournamentMatch rows
     *   -> assembles TournamentBracketDto { tournament, entries, matches }
     */
    @GetMapping("/{uuid}/all")
    public ResponseEntity<TournamentBracketDto> getAll(
            @PathVariable String competitionSlug,
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(tournamentService.getBracket(competitionSlug, uuid));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Rankings (after tournament completes)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns final rankings for all teams after the tournament is complete.
     *
     * Ranking logic:
     *   1st  -> Tournament.firstPlaceEntry (champion)
     *   2nd  -> Tournament.secondPlaceEntry (runner-up from grand final/reset)
     *   3rd+ -> ordered by elimination time (later = better), then by original seed
     *
     * Returns 400 if the tournament is not yet complete.
     *
     * Path:
     *   controller -> TournamentService.getRankings
     *   -> reads Tournament (for 1st/2nd place), TournamentEntry list
     *   -> sorts remaining entries by eliminatedAt DESC, seed ASC
     *   -> maps to List<TournamentRankingDto> with 1-based rank
     */
    @GetMapping("/{uuid}/rankings")
    public ResponseEntity<List<TournamentRankingDto>> getRankings(
            @PathVariable String competitionSlug,
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(tournamentService.getRankings(competitionSlug, uuid));
    }
}
