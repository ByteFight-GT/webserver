package org.bytefight.webserver.tournament;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.ladder.domain.DefaultLadderSettings;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.tournament.application.TournamentBracketBuilder;
import org.bytefight.webserver.tournament.application.TournamentBracketGraph;
import org.bytefight.webserver.tournament.application.TournamentMatchScheduler;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentGame;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.bytefight.webserver.tournament.domain.TournamentStatus;
import org.bytefight.webserver.tournament.infra.TournamentEntryRepository;
import org.bytefight.webserver.tournament.infra.TournamentGameRepository;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import org.bytefight.webserver.tournament.infra.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TournamentMatchSchedulerIntegrationTest extends FullStackIntegrationTestBase {

    @Autowired
    private TournamentBracketBuilder tournamentBracketBuilder;

    @Autowired
    private TournamentMatchScheduler tournamentMatchScheduler;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private LadderRepository ladderRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentEntryRepository tournamentEntryRepository;

    @Autowired
    private TournamentMatchRepository tournamentMatchRepository;

    @Autowired
    private TournamentGameRepository tournamentGameRepository;

    @Test
    void processTournamentForSixTeamsProcessesOnlyReadyMatches() {
        Competition competition = createCompetition("comp-scheduler-process", true);
        Tournament tournament = createTournament(competition);
        createSixSeededEntries(tournament, competition);
        buildAndWireBracket(tournament);

        tournamentMatchScheduler.processTournament(tournament);

        TournamentMatch w1m1 = findMatch(tournament, TournamentBracketType.WINNERS, 1, 1);
        TournamentMatch w1m2 = findMatch(tournament, TournamentBracketType.WINNERS, 1, 2);
        TournamentMatch w1m3 = findMatch(tournament, TournamentBracketType.WINNERS, 1, 3);
        TournamentMatch w1m4 = findMatch(tournament, TournamentBracketType.WINNERS, 1, 4);
        TournamentMatch w2m1 = findMatch(tournament, TournamentBracketType.WINNERS, 2, 1);
        TournamentMatch w2m2 = findMatch(tournament, TournamentBracketType.WINNERS, 2, 2);

        assertEquals(TournamentMatchState.SKIPPED, w1m1.getState());
        assertEquals(1, seedOf(w1m1.getWinnerEntry()));
        assertEquals(TournamentMatchState.SKIPPED, w1m3.getState());
        assertEquals(2, seedOf(w1m3.getWinnerEntry()));

        assertEquals(TournamentMatchState.QUEUED, w1m2.getState());
        assertEquals(4, seedOf(w1m2.getTeamOneEntry()));
        assertEquals(5, seedOf(w1m2.getTeamTwoEntry()));
        assertEquals(1, tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(w1m2).size());

        assertEquals(TournamentMatchState.QUEUED, w1m4.getState());
        assertEquals(3, seedOf(w1m4.getTeamOneEntry()));
        assertEquals(6, seedOf(w1m4.getTeamTwoEntry()));
        assertEquals(1, tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(w1m4).size());

        assertEquals(TournamentMatchState.PENDING, w2m1.getState());
        assertEquals(1, seedOf(w2m1.getTeamOneEntry()));
        assertNull(w2m1.getTeamTwoEntry());

        assertEquals(TournamentMatchState.PENDING, w2m2.getState());
        assertEquals(2, seedOf(w2m2.getTeamOneEntry()));
        assertNull(w2m2.getTeamTwoEntry());

        List<TournamentMatch> queued = tournamentMatchRepository.findByTournamentAndState(tournament, TournamentMatchState.QUEUED);
        assertEquals(2, queued.size());
    }

    @Test
    void processTournamentAfterManualCompletionAdvancesBracket() {
        Competition competition = createCompetition("comp-scheduler-advance", true);
        Tournament tournament = createTournament(competition);
        createSixSeededEntries(tournament, competition);
        buildAndWireBracket(tournament);
        tournamentMatchScheduler.processTournament(tournament);

        TournamentMatch w1m2 = findMatch(tournament, TournamentBracketType.WINNERS, 1, 2);
        TournamentEntry winner = w1m2.getTeamOneEntry();
        TournamentEntry loser = w1m2.getTeamTwoEntry();
        w1m2.setWinnerEntry(winner);
        w1m2.setLoserEntry(loser);
        w1m2.setTeamOneSeriesWins(w1m2.getWinsRequired());
        w1m2.setState(TournamentMatchState.COMPLETE);
        tournamentMatchRepository.save(w1m2);

        tournamentMatchScheduler.advanceFromCompletedMatch(w1m2, winner, loser);
        tournamentMatchScheduler.processTournament(tournament);

        TournamentMatch w2m1 = findMatch(tournament, TournamentBracketType.WINNERS, 2, 1);
        TournamentMatch l1m1 = findMatch(tournament, TournamentBracketType.LOSERS, 1, 1);
        TournamentMatch l2m1 = findMatch(tournament, TournamentBracketType.LOSERS, 2, 1);

        assertEquals(TournamentMatchState.QUEUED, w2m1.getState());
        assertEquals(1, seedOf(w2m1.getTeamOneEntry()));
        assertEquals(4, seedOf(w2m1.getTeamTwoEntry()));
        assertEquals(1, tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(w2m1).size());

        assertEquals(TournamentMatchState.SKIPPED, l1m1.getState());
        assertEquals(5, seedOf(l1m1.getWinnerEntry()));
        assertNull(l1m1.getLoserEntry());

        assertEquals(TournamentMatchState.PENDING, l2m1.getState());
        assertEquals(5, seedOf(l2m1.getTeamOneEntry()));
        assertNull(l2m1.getTeamTwoEntry());

        List<TournamentMatch> queued = tournamentMatchRepository.findByTournamentAndState(tournament, TournamentMatchState.QUEUED);
        assertEquals(2, queued.size());
    }

    @Test
    @Transactional
    void queueSeriesGameUsesUniqueMapsThenFallsBackToEngineChoice() {
        Competition competition = createCompetition("comp-scheduler-maps", true);
        Tournament tournament = createTournament(competition);
        Team teamOne = createTeamWithSubmission(competition, "Team One");
        Team teamTwo = createTeamWithSubmission(competition, "Team Two");

        TournamentEntry entryOne = tournamentEntryRepository.save(createEntry(tournament, teamOne, 1));
        TournamentEntry entryTwo = tournamentEntryRepository.save(createEntry(tournament, teamTwo, 2));

        TournamentMatch match = TournamentMatch.builder()
                .tournament(tournament)
                .bracketType(TournamentBracketType.GRAND_FINAL)
                .roundNumber(1)
                .matchIndex(1)
                .teamOneEntry(entryOne)
                .teamTwoEntry(entryTwo)
                .state(TournamentMatchState.PENDING)
                .seriesLength(TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH)
                .teamOneSeriesWins(0)
                .teamTwoSeriesWins(0)
                .build();
        match = tournamentMatchRepository.save(match);

        for (int i = 0; i < 8; i++) {
            tournamentMatchScheduler.queueSeriesGame(match);
        }

        TournamentMatch refreshed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
        List<TournamentGame> games = tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(refreshed);
        assertEquals(8, games.size());

        List<String> expectedMaps = List.of(
                "the temple",
                "the complex",
                "matrix",
                "maze",
                "spiral",
                "disjoint",
                "big spiral"
        );

        Set<String> expectedMapSet = Set.copyOf(expectedMaps);
        Set<String> usedMapsInSeries = new HashSet<>();
        for (int i = 0; i < expectedMaps.size(); i++) {
            Object mapNameValue = games.get(i).getGameMatch().getMatchSettings().get("map");
            assertTrue(mapNameValue instanceof String, "Map should be present for the first seven games.");
            String mapName = (String) mapNameValue;
            assertTrue(expectedMapSet.contains(mapName), "Map should come from the tournament map pool.");
            assertTrue(usedMapsInSeries.add(mapName), "Map should be unique within the series.");
        }
        assertEquals(expectedMaps.size(), usedMapsInSeries.size(), "All unique maps should be consumed first.");

        assertTrue(games.get(7).getGameMatch().getMatchSettings().isEmpty(),
                "After all unique maps are used, match settings should be empty so engine can choose.");
    }

    private void createSixSeededEntries(Tournament tournament, Competition competition) {
        Team team1 = createTeamWithSubmission(competition, "Seed 1");
        Team team2 = createTeamWithSubmission(competition, "Seed 2");
        Team team3 = createTeamWithSubmission(competition, "Seed 3");
        Team team4 = createTeamWithSubmission(competition, "Seed 4");
        Team team5 = createTeamWithSubmission(competition, "Seed 5");
        Team team6 = createTeamWithSubmission(competition, "Seed 6");

        tournamentEntryRepository.saveAll(List.of(
                createEntry(tournament, team1, 1),
                createEntry(tournament, team2, 2),
                createEntry(tournament, team3, 3),
                createEntry(tournament, team4, 4),
                createEntry(tournament, team5, 5),
                createEntry(tournament, team6, 6)
        ));
    }

    private void buildAndWireBracket(Tournament tournament) {
        List<TournamentEntry> entries = tournamentEntryRepository.findByTournamentOrderBySeed(tournament);
        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);
        tournamentMatchRepository.saveAll(graph.getAllMatches());
        tournamentBracketBuilder.wireWinnersAdvancement(graph.getWinnersRounds());
        tournamentBracketBuilder.wireLosersAdvancement(graph.getWinnersRounds(), graph.getLosersRounds());
        tournamentBracketBuilder.wireLosersToGrandFinal(graph.getWinnersRounds(), graph.getLosersRounds(), graph.getGrandFinal());
        tournamentBracketBuilder.wireGrandFinalReset(graph.getGrandFinal(), graph.getGrandFinalReset());
        tournamentMatchRepository.saveAll(graph.getAllMatches());
    }

    private TournamentMatch findMatch(Tournament tournament, TournamentBracketType type, int round, int index) {
        return tournamentMatchRepository
                .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament)
                .stream()
                .filter(match -> match.getBracketType() == type)
                .filter(match -> match.getRoundNumber() == round)
                .filter(match -> match.getMatchIndex() == index)
                .findFirst()
                .orElseThrow();
    }

    private int seedOf(TournamentEntry entry) {
        return tournamentEntryRepository.findById(entry.getId()).orElseThrow().getSeed();
    }

    private Competition createCompetition(String slug, boolean active) {
        Competition competition = new Competition();
        competition.setSlug(slug + "-" + UUID.randomUUID());
        competition.setName("Competition " + slug);
        competition.setActive(active);
        competition.setWhitelisted(false);
        competition.setMaxPlayersPerTeam(2);
        Competition saved = competitionRepository.save(competition);
        ensureTournamentLadder(saved);
        return saved;
    }

    private void ensureTournamentLadder(Competition competition) {
        if (ladderRepository.findByCompetitionAndLadder(competition, "tournament").isPresent()) {
            return;
        }
        Ladder ladder = DefaultLadderSettings.baseline1500NoInflation();
        ladder.setCompetition(competition);
        ladder.setLadder("tournament");
        ladderRepository.save(ladder);
    }

    private Tournament createTournament(Competition competition) {
        Tournament tournament = Tournament.builder()
                .competition(competition)
                .name("Scheduler Tournament")
                .status(TournamentStatus.IN_PROGRESS)
                .build();
        return tournamentRepository.save(tournament);
    }

    private TournamentEntry createEntry(Tournament tournament, Team team, int seed) {
        return TournamentEntry.builder()
                .tournament(tournament)
                .team(team)
                .seed(seed)
                .build();
    }

    private Team createTeamWithSubmission(Competition competition, String name) {
        Team team = new Team();
        team.setCompetition(competition);
        team.setUuid(UUID.randomUUID());
        team.setName(name);
        team.setDisplayMembers(false);
        team.setJoinCode("JOIN-" + UUID.randomUUID().toString().substring(0, 6));
        teamRepository.save(team);

        FileRecord fileRecord = FileRecord.builder()
                .uuid(UUID.randomUUID())
                .filename("bot.jar")
                .contentType("application/java-archive")
                .size(1L)
                .sha256(UUID.randomUUID().toString())
                .storagePath("/tmp/" + UUID.randomUUID())
                .build();
        fileRecordRepository.save(fileRecord);

        Submission submission = new Submission();
        submission.setUuid(UUID.randomUUID());
        submission.setFileRecord(fileRecord);
        submission.setTeam(team);
        submissionRepository.save(submission);

        team.setCurrentSubmission(submission);
        teamRepository.save(team);
        return team;
    }
}
