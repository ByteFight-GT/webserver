package org.bytefight.webserver.tournament.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentGame;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.bytefight.webserver.tournament.infra.TournamentGameRepository;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import org.springframework.stereotype.Service;

/**
 * Responsible for moving the bracket forward with best-of series support.
 *
 * <p>Key responsibilities: - Auto-advance byes (single-team matches, no series needed) - Queue the
 * FIRST game of a series when both sides are present - Queue the NEXT game of a series when called
 * by the result handler - Place winners/losers into downstream bracket nodes after a series
 * completes
 *
 * <p>This is the bridge between tournament bracket graph and GameMatch queueing.
 *
 * <p>Series flow: 1. processTournament() finds a PENDING match with both teams -> calls
 * queueSeriesGame() 2. queueSeriesGame() creates a GameMatch, a TournamentGame record, and sets
 * state to QUEUED 3. When a game result arrives, TournamentResultHandler updates series scores and
 * either calls queueSeriesGame() again (series not decided) or completes the match
 *
 * <p>Competition-aware behavior: - Only teams from the tournament's competition should appear here
 * - Match creation uses MatchReason.tournament and ladder "tournament"
 */
@Service
@RequiredArgsConstructor
public class TournamentMatchScheduler {
  private static final String MAP_SETTING_KEY = "map";
  private static final String TOURNAMENT_MAPS_SETTING_KEY = "tournamentMaps";

  private final TournamentMatchRepository tournamentMatchRepository;
  private final TournamentGameRepository tournamentGameRepository;
  private final GameMatchService gameMatchService;

  /**
   * Processes the tournament graph until no automatic changes remain.
   *
   * <p>Steps: 1. Mark empty matches (no teams at all) as SKIPPED 2. Auto-advance byes (one team
   * present, other null) to the next winner slot 3. Queue game 1 of any series where both
   * participants are present
   */
  @Transactional
  public void processTournament(Tournament tournament) {
    Map<Long, List<Long>> feederIdsBySlotKey = null;
    boolean changed;
    do {
      changed = false;
      List<TournamentMatch> matches =
          tournamentMatchRepository
              .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament);
      if (feederIdsBySlotKey == null) {
        feederIdsBySlotKey = buildFeederIndex(matches);
      }
      Map<Long, TournamentMatchState> stateByMatchId = buildStateIndex(matches);
      for (TournamentMatch match : matches) {
        if (match.getState() != TournamentMatchState.PENDING) {
          // Only process matches that haven't been queued or completed.
          continue;
        }
        boolean hasTeamOne = match.getTeamOneEntry() != null;
        boolean hasTeamTwo = match.getTeamTwoEntry() != null;
        if (hasTeamOne && hasTeamTwo) {
          // Both teams are present; the match will be queued below.
          continue;
        }
        boolean slotOneResolved = isSlotResolved(match, 1, feederIdsBySlotKey, stateByMatchId);
        boolean slotTwoResolved = isSlotResolved(match, 2, feederIdsBySlotKey, stateByMatchId);
        if (!slotOneResolved || !slotTwoResolved) {
          // At least one feeder path is still unresolved; this is not a true bye yet.
          continue;
        }
        if (!hasTeamOne && !hasTeamTwo) {
          // Both feeder paths resolved to empty. Nothing to advance from this node.
          match.setState(TournamentMatchState.SKIPPED);
          tournamentMatchRepository.save(match);
          stateByMatchId.put(match.getId(), TournamentMatchState.SKIPPED);
          changed = true;
          continue;
        }
        // Bye scenario: single team auto-advances (no series needed).
        TournamentEntry winner = hasTeamOne ? match.getTeamOneEntry() : match.getTeamTwoEntry();
        match.setWinnerEntry(winner);
        match.setState(TournamentMatchState.SKIPPED);
        tournamentMatchRepository.save(match);
        stateByMatchId.put(match.getId(), TournamentMatchState.SKIPPED);
        advanceWinner(match, winner);
        changed = true;
      }
    } while (changed);

    // Queue game 1 for any remaining PENDING matches that now have two participants.
    List<TournamentMatch> pending =
        tournamentMatchRepository.findByTournamentAndState(
            tournament, TournamentMatchState.PENDING);
    for (TournamentMatch match : pending) {
      if (match.getTeamOneEntry() != null && match.getTeamTwoEntry() != null) {
        queueSeriesGame(match);
      }
    }
  }

  private Map<Long, List<Long>> buildFeederIndex(List<TournamentMatch> matches) {
    Map<Long, List<Long>> feederIdsBySlotKey = new HashMap<>();
    for (TournamentMatch match : matches) {
      if (match.getNextWinnerMatchId() != null && match.getNextWinnerSlot() != null) {
        long key = slotKey(match.getNextWinnerMatchId(), match.getNextWinnerSlot());
        feederIdsBySlotKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(match.getId());
      }
      if (match.getNextLoserMatchId() != null && match.getNextLoserSlot() != null) {
        long key = slotKey(match.getNextLoserMatchId(), match.getNextLoserSlot());
        feederIdsBySlotKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(match.getId());
      }
    }
    return feederIdsBySlotKey;
  }

  private Map<Long, TournamentMatchState> buildStateIndex(List<TournamentMatch> matches) {
    Map<Long, TournamentMatchState> stateByMatchId = new HashMap<>();
    for (TournamentMatch match : matches) {
      stateByMatchId.put(match.getId(), match.getState());
    }
    return stateByMatchId;
  }

  private boolean isSlotResolved(
      TournamentMatch target,
      int slot,
      Map<Long, List<Long>> feederIdsBySlotKey,
      Map<Long, TournamentMatchState> stateByMatchId) {
    if (slot == 1 && target.getTeamOneEntry() != null) {
      return true;
    }
    if (slot == 2 && target.getTeamTwoEntry() != null) {
      return true;
    }

    List<Long> feederIds = feederIdsBySlotKey.get(slotKey(target.getId(), slot));
    if (feederIds == null || feederIds.isEmpty()) {
      // No feeder edge into this slot: permanently empty (seeded bye).
      return true;
    }

    for (Long feederId : feederIds) {
      TournamentMatchState state = stateByMatchId.get(feederId);
      if (!isFeederResolved(state)) {
        return false;
      }
    }
    return true;
  }

  private boolean isFeederResolved(TournamentMatchState feederState) {
    return feederState == TournamentMatchState.COMPLETE
        || feederState == TournamentMatchState.SKIPPED;
  }

  private long slotKey(Long matchId, Integer slot) {
    return (matchId << 2) | (slot & 0b11);
  }

  /**
   * Advances entries based on a completed tournament match (series). Winners go to next winner
   * slot; losers go to loser slot (if any).
   *
   * <p>Called by TournamentResultHandler when a series is decided.
   */
  @Transactional
  public void advanceFromCompletedMatch(
      TournamentMatch match, TournamentEntry winner, TournamentEntry loser) {
    advanceWinner(match, winner);
    advanceLoser(match, loser);
  }

  /**
   * Queues the next game in a series.
   *
   * <p>This is the core method for both: - Starting a new series (game 1) when processTournament
   * detects a ready match - Continuing a series (game N+1) when TournamentResultHandler determines
   * the series isn't decided yet (or a draw occurred)
   *
   * <p>Steps: 1. Validate competition scope and submission availability 2. Determine next game
   * number from existing TournamentGame records 3. Create a GameMatch and schedule it via RabbitMQ
   * 4. Create a TournamentGame linking the GameMatch to this series 5. Mark the match as QUEUED
   */
  @Transactional
  public void queueSeriesGame(TournamentMatch match) {
    // Guard: don't queue if the series is already decided.
    if (match.isSeriesDecided()) {
      return;
    }

    Team teamOne = match.getTeamOneEntry().getTeam();
    Team teamTwo = match.getTeamTwoEntry().getTeam();

    // Validate competition scope.
    if (!teamOne.getCompetition().equals(match.getTournament().getCompetition())
        || !teamTwo.getCompetition().equals(match.getTournament().getCompetition())) {
      throw new IllegalArgumentException(
          "Tournament match teams must belong to the tournament competition.");
    }

    Submission submissionOne = teamOne.getCurrentSubmission();
    Submission submissionTwo = teamTwo.getCurrentSubmission();
    if (submissionOne == null || submissionTwo == null) {
      throw new IllegalArgumentException(
          "Tournament match cannot be queued without current submissions.");
    }

    List<TournamentGame> existingGames =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(match);

    // Determine the next game number (1-based).
    int nextGameNumber = existingGames.size() + 1;
    List<String> tournamentMaps = getTournamentMaps(match.getTournament().getCompetition());
    Map<String, Object> matchSettings = buildSeriesMatchSettings(existingGames, tournamentMaps);

    // Create the underlying GameMatch and push it to the queue.
    GameMatch gameMatch =
        gameMatchService.createMatch(
            null, // no creating user for system-generated tournament matches
            teamOne,
            teamTwo,
            submissionOne,
            submissionTwo,
            "tournament",
            MatchReason.tournament,
            matchSettings,
            null);
    gameMatchService.scheduleMatch(gameMatch);

    // Create the TournamentGame record linking this GameMatch to the series.
    TournamentGame game =
        TournamentGame.builder()
            .tournamentMatch(match)
            .gameMatch(gameMatch)
            .gameNumber(nextGameNumber)
            .build();
    tournamentGameRepository.save(game);

    // Mark the match (series) as actively being played.
    match.setState(TournamentMatchState.QUEUED);
    tournamentMatchRepository.save(match);
  }

  /**
   * Reads {@code tournamentMaps} from the competition's settings. Throws if the key is absent or
   * not a List, since map selection cannot proceed without it.
   */
  @SuppressWarnings("unchecked")
  private List<String> getTournamentMaps(Competition competition) {
    Map<String, Object> settings = competition.getSettings();
    Object value = settings == null ? null : settings.get(TOURNAMENT_MAPS_SETTING_KEY);
    if (!(value instanceof List)) {
      throw new IllegalStateException(
          "Competition '"
              + competition.getSlug()
              + "' is missing the required '"
              + TOURNAMENT_MAPS_SETTING_KEY
              + "' setting.");
    }
    return (List<String>) value;
  }

  /**
   * Ensures map uniqueness inside a single best-of series. Chooses randomly from the remaining
   * unused maps. Once all known maps have been used, returns null so the engine can auto-select.
   */
  private Map<String, Object> buildSeriesMatchSettings(
      List<TournamentGame> existingGames, List<String> tournamentMaps) {
    Set<String> usedMaps = new HashSet<>();
    for (TournamentGame game : existingGames) {
      Map<String, Object> settings = game.getGameMatch().getMatchSettings();
      if (settings == null) {
        continue;
      }
      Object value = settings.get(MAP_SETTING_KEY);
      if (value instanceof String mapName) {
        usedMaps.add(mapName);
      }
    }

    List<String> availableMaps = new ArrayList<>();
    for (String mapName : tournamentMaps) {
      if (!usedMaps.contains(mapName)) {
        availableMaps.add(mapName);
      }
    }

    if (!availableMaps.isEmpty()) {
      String selectedMap =
          availableMaps.get(ThreadLocalRandom.current().nextInt(availableMaps.size()));
      return Map.of(MAP_SETTING_KEY, selectedMap);
    }

    return null;
  }

  // ── Internal helpers ────────────────────────────────────────────────────

  private void advanceWinner(TournamentMatch match, TournamentEntry winner) {
    placeEntry(match.getNextWinnerMatchId(), match.getNextWinnerSlot(), winner);
  }

  private void advanceLoser(TournamentMatch match, TournamentEntry loser) {
    placeEntry(match.getNextLoserMatchId(), match.getNextLoserSlot(), loser);
  }

  /**
   * Places an entry into a specific slot of a downstream match. Slot 1 = teamOneEntry, Slot 2 =
   * teamTwoEntry.
   */
  private void placeEntry(Long nextMatchId, Integer slot, TournamentEntry entry) {
    if (nextMatchId == null || slot == null || entry == null) {
      return;
    }
    TournamentMatch target = tournamentMatchRepository.findById(nextMatchId).orElseThrow();
    if (slot == 1) {
      if (target.getTeamOneEntry() == null) {
        target.setTeamOneEntry(entry);
      }
    } else {
      if (target.getTeamTwoEntry() == null) {
        target.setTeamTwoEntry(entry);
      }
    }
    tournamentMatchRepository.save(target);
  }
}
