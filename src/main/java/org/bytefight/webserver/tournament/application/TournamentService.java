package org.bytefight.webserver.tournament.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.tournament.domain.CreateTournamentRequest;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketDto;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentEntryDto;
import org.bytefight.webserver.tournament.domain.TournamentGame;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchDto;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.bytefight.webserver.tournament.domain.TournamentRankingDto;
import org.bytefight.webserver.tournament.domain.TournamentStatus;
import org.bytefight.webserver.tournament.infra.TournamentEntryRepository;
import org.bytefight.webserver.tournament.infra.TournamentGameRepository;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import org.bytefight.webserver.tournament.infra.TournamentRepository;
import org.springframework.stereotype.Service;

/**
 * Orchestrates tournament lifecycle and read APIs.
 *
 * <p>Key responsibilities: - Create tournaments and enroll teams (OPEN) - Build brackets and queue
 * initial matches (IN_PROGRESS) - Provide DTOs for visualization and frontend consumption
 *
 * <p>Storage model: - Tournament is the root table (tournament), scoped to a Competition -
 * TournamentEntry holds team + seed + loss count (tournament_entry) - TournamentMatch holds bracket
 * node graph and linkage (tournament_match)
 *
 * <p>Competition-aware behavior: - All reads/writes are scoped by competition slug - Teams enrolled
 * must belong to the same competition
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TournamentService {
  private static final String DEFAULT_SEED_LADDER = "ranked";

  private final TournamentRepository tournamentRepository;
  private final TournamentEntryRepository tournamentEntryRepository;
  private final TournamentMatchRepository tournamentMatchRepository;
  private final TournamentGameRepository tournamentGameRepository;
  private final GameMatchRepository gameMatchRepository;
  private final TournamentBracketBuilder bracketBuilder;
  private final TournamentMatchScheduler matchScheduler;
  private final TeamService teamService;
  private final CompetitionService competitionService;
  private final TeamStatsRepository teamStatsRepository;
  private final Clock clock;

  /** Loads a competition by slug and normalizes it. */
  public Competition getCompetitionBySlug(String slug) {
    return competitionService
        .getCompetitionBySlug(slug)
        .orElseThrow(() -> new IllegalArgumentException("Competition not found: " + slug));
  }

  /**
   * Loads a tournament by uuid, scoped to a competition. This is the root lookup for all
   * public/admin reads.
   */
  public Tournament getTournamentByUuid(String competitionSlug, String uuid) {
    // Ensures tournament lookups are always scoped to a competition.
    Competition competition = getCompetitionBySlug(competitionSlug);
    return tournamentRepository
        .findByUuidAndCompetition(UUID.fromString(uuid), competition)
        .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + uuid));
  }

  /** Lightweight tournament metadata for UI headers/status panels. */
  public TournamentDto getTournamentDto(String competitionSlug, String uuid) {
    return TournamentDto.from(getTournamentByUuid(competitionSlug, uuid));
  }

  /** Lists all tournaments for a competition, newest first. */
  public List<TournamentDto> getTournaments(String competitionSlug) {
    Competition competition = getCompetitionBySlug(competitionSlug);
    return tournamentRepository.findByCompetitionOrderByIdDesc(competition).stream()
        .map(TournamentDto::from)
        .toList();
  }

  /**
   * Full bracket payload for frontend visualization.
   *
   * <p>Path: - Tournament -> TournamentEntry list (seeded teams) - TournamentMatch list (bracket
   * graph)
   */
  public TournamentBracketDto getBracket(String competitionSlug, String uuid) {
    Tournament tournament = getTournamentByUuid(competitionSlug, uuid);
    // Entries are sorted by seed to help frontend ordering.
    List<TournamentEntryDto> entries =
        tournamentEntryRepository.findByTournamentOrderBySeed(tournament).stream()
            .map(TournamentEntryDto::from)
            .toList();
    // Matches are ordered by bracket type, round, then index for display.
    List<TournamentMatchDto> matches =
        tournamentMatchRepository
            .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament)
            .stream()
            .map(TournamentMatchDto::from)
            .toList();
    return TournamentBracketDto.builder()
        .tournament(TournamentDto.from(tournament))
        .entries(entries)
        .matches(matches)
        .build();
  }

  /**
   * All matches (series) across every bracket type and round. Ordered by bracket type, round
   * number, match index for display.
   */
  public List<TournamentMatchDto> getMatches(String competitionSlug, String uuid) {
    Tournament tournament = getTournamentByUuid(competitionSlug, uuid);
    return tournamentMatchRepository
        .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament)
        .stream()
        .map(TournamentMatchDto::from)
        .toList();
  }

  // ── New public read APIs ────────────────────────────────────────────────

  /**
   * Returns all enrolled teams (entries) for a tournament, ordered by seed.
   *
   * <p>Path: - Tournament -> TournamentEntry list -> TournamentEntryDto list
   */
  public List<TournamentEntryDto> getTeams(String competitionSlug, String uuid) {
    Tournament tournament = getTournamentByUuid(competitionSlug, uuid);
    return tournamentEntryRepository.findByTournamentOrderBySeed(tournament).stream()
        .map(TournamentEntryDto::from)
        .toList();
  }

  /**
   * Returns matches (series) filtered by bracket type and/or round range.
   *
   * <p>All parameters are optional: - bracketType: WINNERS, LOSERS, GRAND_FINAL, GRAND_FINAL_RESET
   * (null = all) - fromRound: inclusive lower bound on roundNumber (null = no lower bound) -
   * toRound: inclusive upper bound on roundNumber (null = no upper bound)
   *
   * <p>Filtering is done in-memory from the full match list since the total number of matches per
   * tournament is bounded (a few hundred max).
   */
  public List<TournamentMatchDto> getMatchesByRound(
      String competitionSlug,
      String uuid,
      TournamentBracketType bracketType,
      Integer fromRound,
      Integer toRound) {
    Tournament tournament = getTournamentByUuid(competitionSlug, uuid);
    return tournamentMatchRepository
        .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament)
        .stream()
        .filter(m -> bracketType == null || m.getBracketType() == bracketType)
        .filter(m -> fromRound == null || m.getRoundNumber() >= fromRound)
        .filter(m -> toRound == null || m.getRoundNumber() <= toRound)
        .map(TournamentMatchDto::from)
        .toList();
  }

  /**
   * Returns final rankings for all teams in a completed tournament.
   *
   * <p>Ranking logic: - 1st place: Tournament.firstPlaceEntry (champion) - 2nd place:
   * Tournament.secondPlaceEntry (runner-up) - Remaining teams: ranked by the losers-bracket round
   * where they were eliminated (later round = better placing, same round = tied placement)
   *
   * <p>Only available when the tournament status is COMPLETE.
   */
  public List<TournamentRankingDto> getRankings(String competitionSlug, String uuid) {
    Tournament tournament = getTournamentByUuid(competitionSlug, uuid);
    if (tournament.getStatus() != TournamentStatus.COMPLETE) {
      throw new IllegalArgumentException(
          "Rankings are only available after the tournament is complete.");
    }

    TournamentEntry first = tournament.getFirstPlaceEntry();
    TournamentEntry second = tournament.getSecondPlaceEntry();
    List<TournamentEntry> allEntries =
        tournamentEntryRepository.findByTournamentOrderBySeed(tournament);

    List<TournamentRankingDto> rankings = new ArrayList<>();
    rankings.add(buildRanking(1, first));
    rankings.add(buildRanking(2, second));

    List<TournamentEntry> remaining =
        allEntries.stream()
            .filter(e -> !e.getId().equals(first.getId()) && !e.getId().equals(second.getId()))
            .toList();

    Map<Long, Integer> eliminationRoundByEntryId = new HashMap<>();
    // TODO:stupid, just get the matches that have bracket type LOSERS from the repo rather than
    // looping over them
    for (TournamentMatch match :
        tournamentMatchRepository.findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(
            tournament)) {
      if (match.getBracketType() != TournamentBracketType.LOSERS
          || match.getState() != TournamentMatchState.COMPLETE
          || match.getLoserEntry() == null
          || match.getRoundNumber() == null) {
        continue;
      }
      Long loserEntryId = match.getLoserEntry().getId();
      Integer existingRound = eliminationRoundByEntryId.get(loserEntryId);
      if (existingRound == null || match.getRoundNumber() > existingRound) {
        eliminationRoundByEntryId.put(loserEntryId, match.getRoundNumber());
      }
    }

    Map<Integer, List<TournamentEntry>> entriesByEliminationRound = new HashMap<>();
    List<TournamentEntry> unknownRound = new ArrayList<>();
    for (TournamentEntry entry : remaining) {
      Integer round = eliminationRoundByEntryId.get(entry.getId());
      if (round == null) {
        unknownRound.add(entry);
      } else {
        entriesByEliminationRound.computeIfAbsent(round, ignored -> new ArrayList<>()).add(entry);
      }
    }

    List<Integer> roundsDesc =
        entriesByEliminationRound.keySet().stream().sorted(Comparator.reverseOrder()).toList();

    int betterPlacedTeams = 2; // champion + runner-up
    for (Integer round : roundsDesc) {
      int rankForRound = betterPlacedTeams + 1;
      List<TournamentEntry> roundEntries =
          entriesByEliminationRound.get(round).stream()
              .sorted(Comparator.comparingInt(TournamentEntry::getSeed))
              .toList();
      for (TournamentEntry entry : roundEntries) {
        rankings.add(buildRanking(rankForRound, entry));
      }
      betterPlacedTeams += roundEntries.size();
    }

    if (!unknownRound.isEmpty()) {
      int fallbackRank = betterPlacedTeams + 1;
      List<TournamentEntry> orderedUnknown =
          unknownRound.stream()
              .sorted(
                  Comparator.comparing(
                          TournamentEntry::getEliminatedAt,
                          Comparator.nullsLast(Comparator.reverseOrder()))
                      .thenComparingInt(TournamentEntry::getSeed))
              .toList();
      for (TournamentEntry entry : orderedUnknown) {
        rankings.add(buildRanking(fallbackRank++, entry));
      }
    }
    return rankings;
  }

  private TournamentRankingDto buildRanking(int rank, TournamentEntry entry) {
    Team team = entry.getTeam();
    return TournamentRankingDto.builder()
        .rank(rank)
        .entryId(entry.getId())
        .teamUuid(team.getUuid().toString())
        .teamName(team.getName())
        .seed(entry.getSeed())
        .losses(entry.getLosses())
        .status(entry.getStatus())
        .build();
  }

  // ── Admin / lifecycle methods ───────────────────────────────────────────

  /** Creates a new tournament and enrolls teams in a single operation. */
  public TournamentDto createTournament(String competitionSlug, CreateTournamentRequest request) {
    Competition competition = getCompetitionBySlug(competitionSlug);
    if (!competition.isActive()) {
      throw new IllegalArgumentException("Competition is not active");
    }

    Tournament tournament =
        Tournament.builder()
            .competition(competition)
            .name(request.getName())
            .status(TournamentStatus.DRAFT)
            .build();
    tournamentRepository.save(tournament);

    List<Team> teams = resolveTeamsForEnrollment(competition, request.getTeamUuids());
    String seedLadder = normalizeSeedLadder(request.getSeedLadder());
    List<TournamentEntry> entries = buildSeededEntries(tournament, competition, teams, seedLadder);
    tournamentEntryRepository.saveAll(entries);

    tournament.setStatus(TournamentStatus.OPEN);
    tournamentRepository.save(tournament);

    return TournamentDto.from(tournament);
  }

  private List<Team> resolveTeamsForEnrollment(Competition competition, List<String> teamUuids) {
    if (teamUuids == null || teamUuids.isEmpty()) {
      // Bulk enroll: all teams with submissions in this competition.
      return teamService.getTeamsWithSubmission(competition);
    }

    List<Team> teams = new ArrayList<>();
    Set<UUID> uniqueTeamUuids = new HashSet<>();
    for (String teamUuidValue : teamUuids) {
      UUID teamUuid = UUID.fromString(teamUuidValue);
      if (!uniqueTeamUuids.add(teamUuid)) {
        throw new IllegalArgumentException("Duplicate team UUID in request: " + teamUuidValue);
      }

      // Explicit enroll: enforce competition scope and submission availability.
      Team team =
          teamService
              .getTeamByCompetitionAndUuid(competition, teamUuid)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Team "
                              + teamUuidValue
                              + " not found in competition "
                              + competition.getSlug()));
      if (team.getCurrentSubmission() == null) {
        throw new IllegalArgumentException(
            "Team " + team.getName() + " has no current submission.");
      }
      teams.add(team);
    }
    return teams;
  }

  private String normalizeSeedLadder(String seedLadder) {
    if (seedLadder == null || seedLadder.trim().isEmpty()) {
      return DEFAULT_SEED_LADDER;
    }
    return seedLadder.trim().toLowerCase();
  }

  private List<TournamentEntry> buildSeededEntries(
      Tournament tournament, Competition competition, List<Team> teams, String seedLadder) {
    if (teams.isEmpty()) {
      return List.of();
    }

    // Seed by rank using glicko rating on the specified ladder; fall back to name for
    // ties/unranked.
    Map<Long, Double> ratingByTeamId = new HashMap<>();
    List<TeamStats> stats =
        teamStatsRepository.findAllByCompetitionAndLadderAndTeamIn(competition, seedLadder, teams);
    for (TeamStats teamStats : stats) {
      ratingByTeamId.put(teamStats.getTeam().getId(), teamStats.getGlickoRating());
    }

    teams.sort(
        Comparator.<Team>comparingDouble(
                team -> ratingByTeamId.getOrDefault(team.getId(), Double.NEGATIVE_INFINITY))
            .reversed()
            .thenComparing(Team::getNameNormalized));

    List<TournamentEntry> entries = new ArrayList<>();
    int seed = 1;
    for (Team team : teams) {
      // Each entry links a team to the tournament and tracks losses.
      TournamentEntry entry =
          TournamentEntry.builder().tournament(tournament).team(team).seed(seed++).build();
      entries.add(entry);
    }
    return entries;
  }

  /**
   * Starts the tournament: - builds double-elimination bracket nodes - wires winner/loser
   * advancement graph - queues initial matches into GameMatch queue
   */
  public TournamentBracketDto startTournament(String competitionSlug, String tournamentUuid) {
    Tournament tournament = getTournamentByUuid(competitionSlug, tournamentUuid);
    if (tournament.getStatus() == TournamentStatus.IN_PROGRESS
        || tournament.getStatus() == TournamentStatus.COMPLETE) {
      throw new IllegalArgumentException("Tournament already started or completed.");
    }
    if (!tournament.getCompetition().isActive()) {
      throw new IllegalArgumentException("Competition is not active");
    }
    List<TournamentEntry> entries =
        tournamentEntryRepository.findByTournamentOrderBySeed(tournament);
    if (entries.size() < 2) {
      throw new IllegalArgumentException("Tournament must have at least 2 teams.");
    }

    // Build bracket graph in memory, then persist nodes and edges.
    TournamentBracketGraph graph = bracketBuilder.buildBracket(tournament, entries);
    tournamentMatchRepository.saveAll(graph.getAllMatches());
    bracketBuilder.wireWinnersAdvancement(graph.getWinnersRounds());
    bracketBuilder.wireLosersAdvancement(graph.getWinnersRounds(), graph.getLosersRounds());
    bracketBuilder.wireLosersToGrandFinal(
        graph.getWinnersRounds(), graph.getLosersRounds(), graph.getGrandFinal());
    bracketBuilder.wireGrandFinalReset(graph.getGrandFinal(), graph.getGrandFinalReset());
    tournamentMatchRepository.saveAll(graph.getAllMatches());

    // Mark tournament live and queue initial matches.
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    tournament.setStartedAt(LocalDateTime.now(clock));
    tournamentRepository.save(tournament);

    matchScheduler.processTournament(tournament);
    return getBracket(competitionSlug, tournamentUuid);
  }

  /** Terminates a tournament synchronously without deleting any tournament data. */
  public void terminateTournament(String competitionSlug, String tournamentUuid) {
    Tournament tournament = getTournamentByUuid(competitionSlug, tournamentUuid);
    tournament =
        tournamentRepository
            .findByIdForUpdate(tournament.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("Tournament not found: " + tournamentUuid));

    if (tournament.getStatus() == TournamentStatus.COMPLETE) {
      return;
    }

    Instant finishedAt = Instant.now(clock);
    for (TournamentMatch match :
        tournamentMatchRepository.findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(
            tournament)) {
      match.setState(TournamentMatchState.SKIPPED);
      tournamentMatchRepository.save(match);
      for (TournamentGame game :
          tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(match)) {
        game.getGameMatch().setStatus(MatchStatus.failed);
        game.getGameMatch().setFinishedAt(finishedAt);
        game.setResultProcessed(true);
        gameMatchRepository.save(game.getGameMatch());
        tournamentGameRepository.save(game);
      }
    }

    tournament.setStatus(TournamentStatus.COMPLETE);
    tournament.setFinishedAt(LocalDateTime.now(clock));
    tournamentRepository.save(tournament);
  }
}
