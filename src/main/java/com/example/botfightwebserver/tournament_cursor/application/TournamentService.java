package com.example.botfightwebserver.tournament_cursor.application;

import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.tournament_cursor.domain.CreateTournamentRequest;
import com.example.botfightwebserver.tournament_cursor.domain.EnrollTeamsRequest;
import com.example.botfightwebserver.tournament_cursor.domain.Tournament;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentBracketDto;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentDto;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentEntry;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentEntryDto;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentMatchDto;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentStatus;
import com.example.botfightwebserver.tournament_cursor.infra.TournamentEntryRepository;
import com.example.botfightwebserver.tournament_cursor.infra.TournamentMatchRepository;
import com.example.botfightwebserver.tournament_cursor.infra.TournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
 * - Tournament is the root table (tournament_cursor)
 * - TournamentEntry holds team + seed + loss count (tournament_cursor_entry)
 * - TournamentMatch holds bracket node graph and linkage (tournament_cursor_match)
 */
@Service
@RequiredArgsConstructor
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentBracketBuilder bracketBuilder;
    private final TournamentMatchScheduler matchScheduler;
    private final TeamService teamService;
    private final Clock clock;

    /**
     * Loads a tournament by uuid.
     * This is the root lookup for all public/admin reads.
     */
    public Tournament getTournamentByUuid(String uuid) {
        return tournamentRepository.findByUuid(UUID.fromString(uuid)).orElseThrow();
    }

    /**
     * Lightweight tournament metadata for UI headers/status panels.
     */
    public TournamentDto getTournamentDto(String uuid) {
        return TournamentDto.from(getTournamentByUuid(uuid));
    }

    /**
     * Full bracket payload for frontend visualization.
     *
     * Path:
     * - Tournament -> TournamentEntry list (seeded teams)
     * - TournamentMatch list (bracket graph)
     */
    public TournamentBracketDto getBracket(String uuid) {
        Tournament tournament = getTournamentByUuid(uuid);
        List<TournamentEntryDto> entries = tournamentEntryRepository.findByTournamentOrderBySeed(tournament)
                .stream()
                .map(TournamentEntryDto::from)
                .toList();
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
    public List<TournamentMatchDto> getMatches(String uuid) {
        Tournament tournament = getTournamentByUuid(uuid);
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
    public TournamentDto createTournament(CreateTournamentRequest request) {
        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .maxTeams(request.getMaxTeams())
                .status(TournamentStatus.DRAFT)
                .build();
        tournamentRepository.save(tournament);
        return TournamentDto.from(tournament);
    }

    /**
     * Enrolls teams and assigns seeds by descending glicko.
     *
     * Path:
     * - resolve team list (explicit UUIDs or all teams with submissions)
     * - create TournamentEntry rows (seeded)
     * - move tournament to OPEN
     */
    @Transactional
    public List<TournamentEntryDto> enrollTeams(String tournamentUuid, EnrollTeamsRequest request) {
        Tournament tournament = getTournamentByUuid(tournamentUuid);
        if (tournament.getStatus() != TournamentStatus.DRAFT && tournament.getStatus() != TournamentStatus.OPEN) {
            throw new IllegalArgumentException("Tournament is not open for enrollment.");
        }

        List<Team> teams;
        if (request == null || request.getTeamUuids() == null || request.getTeamUuids().isEmpty()) {
            teams = teamService.getTeamsWithSubmission();
        } else {
            teams = new ArrayList<>();
            for (String teamUuid : request.getTeamUuids()) {
                Team team = teamService.getTeamByUuid(teamUuid).orElseThrow();
                if (team.getCurrentSubmission() == null) {
                    throw new IllegalArgumentException("Team " + team.getName() + " has no current submission.");
                }
                teams.add(team);
            }
        }

        if (tournament.getMaxTeams() != null && teams.size() > tournament.getMaxTeams()) {
            throw new IllegalArgumentException("Too many teams for this tournament.");
        }

        teams.sort(Comparator.comparing(Team::getGlicko).reversed());

        List<TournamentEntry> entries = new ArrayList<>();
        int seed = 1;
        for (Team team : teams) {
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
    public TournamentBracketDto startTournament(String tournamentUuid) {
        Tournament tournament = getTournamentByUuid(tournamentUuid);
        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS || tournament.getStatus() == TournamentStatus.COMPLETE) {
            throw new IllegalArgumentException("Tournament already started or completed.");
        }
        List<TournamentEntry> entries = tournamentEntryRepository.findByTournamentOrderBySeed(tournament);
        if (entries.size() < 2) {
            throw new IllegalArgumentException("Tournament must have at least 2 teams.");
        }

        TournamentBracketGraph graph = bracketBuilder.buildBracket(tournament, entries);
        tournamentMatchRepository.saveAll(graph.getAllMatches());
        bracketBuilder.wireWinnersAdvancement(graph.getWinnersRounds());
        bracketBuilder.wireLosersAdvancement(graph.getWinnersRounds(), graph.getLosersRounds());
        bracketBuilder.wireLosersToGrandFinal(graph.getWinnersRounds(), graph.getLosersRounds(), graph.getGrandFinal());
        bracketBuilder.wireGrandFinalReset(graph.getGrandFinal(), graph.getGrandFinalReset());
        tournamentMatchRepository.saveAll(graph.getAllMatches());

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setStartedAt(LocalDateTime.now(clock));
        tournamentRepository.save(tournament);

        matchScheduler.processTournament(tournament);
        return getBracket(tournamentUuid);
    }
}
