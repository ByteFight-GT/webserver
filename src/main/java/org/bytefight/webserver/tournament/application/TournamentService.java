package org.bytefight.webserver.tournament.application;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.tournament.domain.CreateTournamentRequest;
import org.bytefight.webserver.tournament.domain.EnrollTeamsRequest;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketDto;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentEntryDto;
import org.bytefight.webserver.tournament.domain.TournamentMatchDto;
import org.bytefight.webserver.tournament.domain.TournamentStatus;
import org.bytefight.webserver.tournament.infra.TournamentEntryRepository;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import org.bytefight.webserver.tournament.infra.TournamentRepository;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates tournament lifecycle and read APIs.
 *
 * Key responsibilities:
 * - Create tournaments (DRAFT)
 * - Enroll teams and assign seeds (OPEN)
 * - Build brackets and queue initial matches (IN_PROGRESS)
 * - Provide DTOs for visualization and frontend consumption
 *
 * Storage model:
 * - Tournament is the root table (tournament), scoped to a Competition
 * - TournamentEntry holds team + seed + loss count (tournament_entry)
 * - TournamentMatch holds bracket node graph and linkage (tournament_match)
 *
 * Competition-aware behavior:
 * - All reads/writes are scoped by competition slug
 * - Teams enrolled must belong to the same competition
 */
@Service
@RequiredArgsConstructor
public class TournamentService {
    private static final String DEFAULT_SEED_LADDER = "tournament";

    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentBracketBuilder bracketBuilder;
    private final TournamentMatchScheduler matchScheduler;
    private final TeamService teamService;
    private final CompetitionService competitionService;
    private final TeamStatsRepository teamStatsRepository;
    private final Clock clock;

    /**
     * Loads a competition by slug and normalizes it.
     */
    public Competition getCompetitionBySlug(String slug) {
        return competitionService.getCompetitionBySlug(slug).orElseThrow();
    }

    /**
     * Loads a tournament by uuid, scoped to a competition.
     * This is the root lookup for all public/admin reads.
     */
    public Tournament getTournamentByUuid(String competitionSlug, String uuid) {
        // Ensures tournament lookups are always scoped to a competition.
        Competition competition = getCompetitionBySlug(competitionSlug);
        return tournamentRepository.findByUuidAndCompetition(UUID.fromString(uuid), competition).orElseThrow();
    }

    /**
     * Lightweight tournament metadata for UI headers/status panels.
     */
    public TournamentDto getTournamentDto(String competitionSlug, String uuid) {
        return TournamentDto.from(getTournamentByUuid(competitionSlug, uuid));
    }

    /**
     * Full bracket payload for frontend visualization.
     *
     * Path:
     * - Tournament -> TournamentEntry list (seeded teams)
     * - TournamentMatch list (bracket graph)
     */
    public TournamentBracketDto getBracket(String competitionSlug, String uuid) {
        Tournament tournament = getTournamentByUuid(competitionSlug, uuid);
        // Entries are sorted by seed to help frontend ordering.
        List<TournamentEntryDto> entries = tournamentEntryRepository.findByTournamentOrderBySeed(tournament)
                .stream()
                .map(TournamentEntryDto::from)
                .toList();
        // Matches are ordered by bracket type, round, then index for display.
        List<TournamentMatchDto> matches = tournamentMatchRepository
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
     * Matches-only payload for timeline or bracket rendering.
     */
    public List<TournamentMatchDto> getMatches(String competitionSlug, String uuid) {
        Tournament tournament = getTournamentByUuid(competitionSlug, uuid);
        return tournamentMatchRepository
                .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament)
                .stream()
                .map(TournamentMatchDto::from)
                .toList();
    }

    /**
     * Creates a new tournament in DRAFT state.
     * No teams or matches are created here.
     */
    @Transactional
    public TournamentDto createTournament(String competitionSlug, CreateTournamentRequest request) {
        Competition competition = getCompetitionBySlug(competitionSlug);
        // Tournaments are always created within a competition scope.
        Tournament tournament = Tournament.builder()
                .competition(competition)
                .name(request.getName())
                .maxTeams(request.getMaxTeams())
                .status(TournamentStatus.DRAFT)
                .build();
        tournamentRepository.save(tournament);
        return TournamentDto.from(tournament);
    }

    /**
     * Enrolls teams and assigns deterministic seeds by team rank.
     *
     * Path:
     * - resolve team list (explicit UUIDs or all teams with submissions)
     * - create TournamentEntry rows (seeded)
     * - move tournament to OPEN
     */
    @Transactional
    public List<TournamentEntryDto> enrollTeams(String competitionSlug, String tournamentUuid, EnrollTeamsRequest request) {
        Tournament tournament = getTournamentByUuid(competitionSlug, tournamentUuid);
        if (tournament.getStatus() != TournamentStatus.DRAFT && tournament.getStatus() != TournamentStatus.OPEN) {
            throw new IllegalArgumentException("Tournament is not open for enrollment.");
        }
        Competition competition = tournament.getCompetition();
        if (!competition.isActive()) {
            throw new IllegalArgumentException("Competition is not active");
        }

        List<Team> teams;
        if (request == null || request.getTeamUuids() == null || request.getTeamUuids().isEmpty()) {
            // Bulk enroll: all teams with submissions in this competition.
            teams = teamService.getTeamsWithSubmission(competition);
        } else {
            teams = new ArrayList<>();
            for (String teamUuid : request.getTeamUuids()) {
                // Explicit enroll: enforce competition scope and submission availability.
                Team team = teamService.getTeamByCompetitionAndUuid(competition, UUID.fromString(teamUuid)).orElseThrow();
                if (team.getCurrentSubmission() == null) {
                    throw new IllegalArgumentException("Team " + team.getName() + " has no current submission.");
                }
                teams.add(team);
            }
        }

        if (tournament.getMaxTeams() != null && teams.size() > tournament.getMaxTeams()) {
            throw new IllegalArgumentException("Too many teams for this tournament.");
        }

        // Seed by rank using glicko rating on the specified ladder; fall back to name for ties/unranked.
        String seedLadder = (request != null && request.getSeedLadder() != null && !request.getSeedLadder().isBlank())
                ? request.getSeedLadder().trim().toLowerCase()
                : DEFAULT_SEED_LADDER;
        Map<Long, Double> ratingByTeamId = new HashMap<>();
        List<TeamStats> stats = teamStatsRepository.findByCompetitionAndLadderAndTeamIn(competition, seedLadder, teams);
        for (TeamStats teamStats : stats) {
            ratingByTeamId.put(teamStats.getTeam().getId(), teamStats.getGlickoRating());
        }

        teams.sort(
                Comparator.<Team>comparingDouble(team -> ratingByTeamId.getOrDefault(team.getId(), Double.NEGATIVE_INFINITY))
                        .reversed()
                        .thenComparing(Team::getNameNormalized)
        );

        List<TournamentEntry> entries = new ArrayList<>();
        int seed = 1;
        for (Team team : teams) {
            // Each entry links a team to the tournament and tracks losses.
            TournamentEntry entry = TournamentEntry.builder()
                    .tournament(tournament)
                    .team(team)
                    .seed(seed++)
                    .build();
            entries.add(entry);
        }

        List<TournamentEntry> saved = tournamentEntryRepository.saveAll(entries);
        tournament.setStatus(TournamentStatus.OPEN);
        tournamentRepository.save(tournament);

        return saved.stream().map(TournamentEntryDto::from).toList();
    }

    /**
     * Starts the tournament:
     * - builds double-elimination bracket nodes
     * - wires winner/loser advancement graph
     * - queues initial matches into GameMatch queue
     */
    @Transactional
    public TournamentBracketDto startTournament(String competitionSlug, String tournamentUuid) {
        Tournament tournament = getTournamentByUuid(competitionSlug, tournamentUuid);
        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS || tournament.getStatus() == TournamentStatus.COMPLETE) {
            throw new IllegalArgumentException("Tournament already started or completed.");
        }
        if (!tournament.getCompetition().isActive()) {
            throw new IllegalArgumentException("Competition is not active");
        }
        List<TournamentEntry> entries = tournamentEntryRepository.findByTournamentOrderBySeed(tournament);
        if (entries.size() < 2) {
            throw new IllegalArgumentException("Tournament must have at least 2 teams.");
        }

        // Build bracket graph in memory, then persist nodes and edges.
        TournamentBracketGraph graph = bracketBuilder.buildBracket(tournament, entries);
        tournamentMatchRepository.saveAll(graph.getAllMatches());
        bracketBuilder.wireWinnersAdvancement(graph.getWinnersRounds());
        bracketBuilder.wireLosersAdvancement(graph.getWinnersRounds(), graph.getLosersRounds());
        bracketBuilder.wireLosersToGrandFinal(graph.getWinnersRounds(), graph.getLosersRounds(), graph.getGrandFinal());
        bracketBuilder.wireGrandFinalReset(graph.getGrandFinal(), graph.getGrandFinalReset());
        tournamentMatchRepository.saveAll(graph.getAllMatches());

        // Mark tournament live and queue initial matches.
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setStartedAt(LocalDateTime.now(clock));
        tournamentRepository.save(tournament);

        matchScheduler.processTournament(tournament);
        return getBracket(competitionSlug, tournamentUuid);
    }
}
