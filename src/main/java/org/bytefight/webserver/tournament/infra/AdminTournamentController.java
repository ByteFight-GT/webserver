package org.bytefight.webserver.tournament.infra;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.tournament.application.TournamentService;
import org.bytefight.webserver.tournament.domain.CreateTournamentRequest;
import org.bytefight.webserver.tournament.domain.TournamentBracketDto;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import org.bytefight.webserver.tournament.domain.TournamentEntryDto;
import org.bytefight.webserver.tournament.domain.TournamentMatchDto;
import org.bytefight.webserver.tournament.domain.TournamentRankingDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoints for tournament lifecycle management, scoped to a competition.
 *
 * <p>Data flow summary: - HTTP request -> controller -> TournamentService -> repositories + bracket
 * builder/scheduler - Match queueing uses GameMatchService via TournamentMatchScheduler - Results
 * come back through GameMatchResultHandler -> TournamentResultHandler
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/competition/{competitionSlug}/tournament")
public class AdminTournamentController {
  private final TournamentService tournamentService;

  /**
   * Returns tournament metadata: status, sizes, timestamps, and 1st/2nd place (if the tournament is
   * complete).
   */
  @GetMapping("/{uuid}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TournamentDto> getTournament(
      @PathVariable String competitionSlug, @PathVariable String uuid) {
    return ResponseEntity.ok(tournamentService.getTournamentDto(competitionSlug, uuid));
  }

  /** Returns all enrolled teams (tournament entries) sorted by seed. */
  @GetMapping("/{uuid}/teams")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TournamentEntryDto>> getTeams(
      @PathVariable String competitionSlug, @PathVariable String uuid) {
    return ResponseEntity.ok(tournamentService.getTeams(competitionSlug, uuid));
  }

  /** Returns matches (series) with optional filtering by bracket type and round range. */
  @GetMapping("/{uuid}/rounds")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TournamentMatchDto>> getRounds(
      @PathVariable String competitionSlug,
      @PathVariable String uuid,
      @RequestParam(required = false) TournamentBracketType bracketType,
      @RequestParam(required = false) Integer fromRound,
      @RequestParam(required = false) Integer toRound) {
    return ResponseEntity.ok(
        tournamentService.getMatchesByRound(
            competitionSlug, uuid, bracketType, fromRound, toRound));
  }

  /** Returns the full tournament payload: metadata, all enrolled teams, and all matches. */
  @GetMapping("/{uuid}/all")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TournamentBracketDto> getAll(
      @PathVariable String competitionSlug, @PathVariable String uuid) {
    return ResponseEntity.ok(tournamentService.getBracket(competitionSlug, uuid));
  }

  /** Returns final rankings for all teams after the tournament is complete. */
  @GetMapping("/{uuid}/rankings")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TournamentRankingDto>> getRankings(
      @PathVariable String competitionSlug, @PathVariable String uuid) {
    return ResponseEntity.ok(tournamentService.getRankings(competitionSlug, uuid));
  }

  /** Returns all tournaments for a competition (newest first). */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TournamentDto>> getTournaments(@PathVariable String competitionSlug) {
    return ResponseEntity.ok(tournamentService.getTournaments(competitionSlug));
  }

  /**
   * Creates a tournament and enrolls teams for the given competition.
   *
   * <p>Path: - request body -> TournamentService.createTournament - persists Tournament in
   * tournament table - resolves and seeds tournament entries in tournament_entry table - updates
   * tournament status to OPEN - returns TournamentDto for frontend/admin confirmation
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TournamentDto> createTournament(
      @PathVariable String competitionSlug, @Valid @RequestBody CreateTournamentRequest request) {
    return ResponseEntity.ok(tournamentService.createTournament(competitionSlug, request));
  }

  /**
   * Starts a tournament, builds the bracket, and queues all initial matches.
   *
   * <p>Path: - controller -> TournamentService.startTournament - bracket generation via
   * TournamentBracketBuilder - TournamentMatch rows created (tournament_match) -
   * TournamentMatchScheduler queues matches using GameMatchService (MatchReason.tournament)
   */
  @PostMapping("/{uuid}/start")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TournamentBracketDto> startTournament(
      @PathVariable String competitionSlug, @PathVariable String uuid) {
    return ResponseEntity.ok(tournamentService.startTournament(competitionSlug, uuid));
  }

  /** Terminates the tournament and fails all of its games. */
  @DeleteMapping("/{uuid}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> terminateTournament(
      @PathVariable String competitionSlug, @PathVariable String uuid) {
    tournamentService.terminateTournament(competitionSlug, uuid);
    return ResponseEntity.noContent().build();
  }
}
