package org.bytefight.webserver.tournament;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.ladder.domain.DefaultLadderSettings;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.tournament.application.TournamentBracketBuilder;
import org.bytefight.webserver.tournament.application.TournamentService;
import org.bytefight.webserver.tournament.domain.CreateTournamentRequest;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentEntryStatus;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.bytefight.webserver.tournament.domain.TournamentRankingDto;
import org.bytefight.webserver.tournament.domain.TournamentStatus;
import org.bytefight.webserver.tournament.infra.TournamentEntryRepository;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import org.bytefight.webserver.tournament.infra.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TournamentServiceIntegrationTest extends FullStackIntegrationTestBase {

    @Autowired
    private TournamentService tournamentService;

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

    @Test
    void createTournamentIsCompetitionScoped() {
        Competition competition = createCompetition("comp-create", true);
        createTeam(competition, "Alpha", true);
        createTeam(competition, "Beta", false);
        CreateTournamentRequest request = createTournamentRequest("Winter Cup", null);

        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);
        Tournament tournament = tournamentRepository.findByUuidAndCompetition(UUID.fromString(dto.getUuid()), competition).orElseThrow();

        assertEquals(competition.getId(), tournament.getCompetition().getId());
        assertEquals(TournamentStatus.OPEN, tournament.getStatus());
        assertEquals(1, tournamentEntryRepository.countByTournament(tournament));
    }

    @Test
    void createTournamentRequiresActiveCompetition() {
        Competition competition = createCompetition("comp-inactive", false);
        CreateTournamentRequest request = createTournamentRequest("Inactive Cup", null);

        assertThrows(IllegalArgumentException.class, () ->
                tournamentService.createTournament(competition.getSlug(), request));
    }

    @Test
    void createTournamentRejectsTeamWithoutSubmission() {
        Competition competition = createCompetition("comp-nosub", true);
        Team team = createTeam(competition, "Alpha", false);
        CreateTournamentRequest request = createTournamentRequest("NoSub Cup", List.of(team.getUuid().toString()));

        assertThrows(IllegalArgumentException.class, () ->
                tournamentService.createTournament(competition.getSlug(), request));
    }

    @Test
    void createTournamentRejectsDuplicateTeamUuids() {
        Competition competition = createCompetition("comp-max", true);
        Team team = createTeam(competition, "Alpha", true);
        CreateTournamentRequest request = createTournamentRequest(
                "Duplicate Teams Cup",
                List.of(team.getUuid().toString(), team.getUuid().toString())
        );

        assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(competition.getSlug(), request));
    }

    @Test
    void startTournamentRequiresAtLeastTwoTeams() {
        Competition competition = createCompetition("comp-min", true);
        Team team = createTeam(competition, "Alpha", true);
        CreateTournamentRequest request = createTournamentRequest("Solo Cup", List.of(team.getUuid().toString()));
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);

        assertThrows(IllegalArgumentException.class, () ->
                tournamentService.startTournament(competition.getSlug(), dto.getUuid()));
    }

    @Test
    void startTournamentBuildsBracketAndQueuesMatches() {
        Competition competition = createCompetition("comp-start", true);
        Team teamA = createTeam(competition, "Alpha", true);
        Team teamB = createTeam(competition, "Beta", true);
        Team teamC = createTeam(competition, "Gamma", true);
        Team teamD = createTeam(competition, "Delta", true);
        CreateTournamentRequest request = createTournamentRequest("Bracket Cup", List.of(
                teamA.getUuid().toString(),
                teamB.getUuid().toString(),
                teamC.getUuid().toString(),
                teamD.getUuid().toString()
        ));
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);
        tournamentService.startTournament(competition.getSlug(), dto.getUuid());

        Tournament tournament = tournamentRepository.findByUuidAndCompetition(UUID.fromString(dto.getUuid()), competition).orElseThrow();
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        assertEquals(4, tournament.getBracketSize());

        List<TournamentMatch> allMatches = tournamentMatchRepository
                .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament);
        assertTrue(allMatches.size() > 0);

        List<TournamentMatch> queued = tournamentMatchRepository
                .findByTournamentAndState(tournament, TournamentMatchState.QUEUED);
        assertTrue(queued.size() > 0, "At least one series should be queued (game 1)");
    }

    @Test
    void startTournamentAutoAdvancesByes() {
        Competition competition = createCompetition("comp-bye", true);
        Team teamA = createTeam(competition, "Alpha", true);
        Team teamB = createTeam(competition, "Beta", true);
        Team teamC = createTeam(competition, "Gamma", true);
        CreateTournamentRequest request = createTournamentRequest("Bye Cup", List.of(
                teamA.getUuid().toString(),
                teamB.getUuid().toString(),
                teamC.getUuid().toString()
        ));
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);
        tournamentService.startTournament(competition.getSlug(), dto.getUuid());

        Tournament tournament = tournamentRepository.findByUuidAndCompetition(UUID.fromString(dto.getUuid()), competition).orElseThrow();
        assertTrue(tournamentMatchRepository.findByTournamentAndState(tournament, TournamentMatchState.SKIPPED).size() > 0);
    }

    /**
     * Verifies that bracket matches are assigned the correct series lengths:
     * - Winners/Losers bracket matches: Bo5
     * - Grand final and reset: Bo7
     */
    @Test
    void startTournamentAssignsCorrectSeriesLengths() {
        Competition competition = createCompetition("comp-series", true);
        Team teamA = createTeam(competition, "Alpha", true);
        Team teamB = createTeam(competition, "Beta", true);
        Team teamC = createTeam(competition, "Gamma", true);
        Team teamD = createTeam(competition, "Delta", true);
        CreateTournamentRequest request = createTournamentRequest("Series Cup", List.of(
                teamA.getUuid().toString(),
                teamB.getUuid().toString(),
                teamC.getUuid().toString(),
                teamD.getUuid().toString()
        ));
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);
        tournamentService.startTournament(competition.getSlug(), dto.getUuid());

        Tournament tournament = tournamentRepository.findByUuidAndCompetition(UUID.fromString(dto.getUuid()), competition).orElseThrow();
        List<TournamentMatch> allMatches = tournamentMatchRepository
                .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament);

        for (TournamentMatch match : allMatches) {
            assertNotNull(match.getSeriesLength(), "Series length should be set for match " + match.getId());
            if (match.getBracketType() == TournamentBracketType.GRAND_FINAL
                    || match.getBracketType() == TournamentBracketType.GRAND_FINAL_RESET) {
                assertEquals(TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH, match.getSeriesLength(),
                        "Grand final matches should be Bo" + TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH);
            } else {
                assertEquals(TournamentBracketBuilder.NORMAL_SERIES_LENGTH, match.getSeriesLength(),
                        "Regular bracket matches should be Bo" + TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
            }
            // Series wins should be initialized to 0
            assertEquals(0, match.getTeamOneSeriesWins());
            assertEquals(0, match.getTeamTwoSeriesWins());
        }
    }

    @Test
    void getRankingsUsesLosersRoundWithTiedPlacements() {
        Competition competition = createCompetition("comp-rankings-round", true);
        Tournament tournament = Tournament.builder()
                .competition(competition)
                .name("Rankings Round Cup")
                .status(TournamentStatus.COMPLETE)
                .build();
        tournamentRepository.save(tournament);

        TournamentEntry seed1 = createEntry(tournament, createTeam(competition, "Seed 1", false), 1);
        TournamentEntry seed2 = createEntry(tournament, createTeam(competition, "Seed 2", false), 2);
        TournamentEntry seed3 = createEntry(tournament, createTeam(competition, "Seed 3", false), 3);
        TournamentEntry seed4 = createEntry(tournament, createTeam(competition, "Seed 4", false), 4);
        TournamentEntry seed5 = createEntry(tournament, createTeam(competition, "Seed 5", false), 5);
        TournamentEntry seed6 = createEntry(tournament, createTeam(competition, "Seed 6", false), 6);
        TournamentEntry seed7 = createEntry(tournament, createTeam(competition, "Seed 7", false), 7);
        TournamentEntry seed8 = createEntry(tournament, createTeam(competition, "Seed 8", false), 8);

        markEliminated(seed3);
        markEliminated(seed4);
        markEliminated(seed5);
        markEliminated(seed6);
        markEliminated(seed7);
        markEliminated(seed8);

        tournament.setFirstPlaceEntry(seed1);
        tournament.setSecondPlaceEntry(seed2);
        tournamentRepository.save(tournament);

        // 8-team expected placement by losers elimination round:
        // L4 -> 3rd, L3 -> 4th, L2 -> tied 5th, L1 -> tied 7th
        createCompletedLosersEliminationMatch(tournament, 4, 1, seed3);
        createCompletedLosersEliminationMatch(tournament, 3, 1, seed4);
        createCompletedLosersEliminationMatch(tournament, 2, 1, seed5);
        createCompletedLosersEliminationMatch(tournament, 2, 2, seed6);
        createCompletedLosersEliminationMatch(tournament, 1, 1, seed7);
        createCompletedLosersEliminationMatch(tournament, 1, 2, seed8);

        List<TournamentRankingDto> rankings = tournamentService.getRankings(
                competition.getSlug(),
                tournament.getUuid().toString()
        );

        assertEquals(8, rankings.size());
        assertSeedRank(rankings, 1, 1);
        assertSeedRank(rankings, 2, 2);
        assertSeedRank(rankings, 3, 3);
        assertSeedRank(rankings, 4, 4);
        assertSeedRank(rankings, 5, 5);
        assertSeedRank(rankings, 6, 5);
        assertSeedRank(rankings, 7, 7);
        assertSeedRank(rankings, 8, 7);
    }

    // ── Helper methods ──────────────────────────────────────────────────────

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

    private CreateTournamentRequest createTournamentRequest(String name, List<String> teamUuids) {
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName(name);
        request.setTeamUuids(teamUuids);
        request.setSeedLadder("ranked");
        return request;
    }

    private Team createTeam(Competition competition, String name, boolean withSubmission) {
        Team team = new Team();
        team.setCompetition(competition);
        team.setUuid(UUID.randomUUID());
        team.setName(name);
        team.setDisplayMembers(false);
        team.setJoinCode("JOIN-" + UUID.randomUUID().toString().substring(0, 6));
        teamRepository.save(team);

        if (withSubmission) {
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
        }

        return team;
    }

    private TournamentEntry createEntry(Tournament tournament, Team team, int seed) {
        return tournamentEntryRepository.save(
                TournamentEntry.builder()
                        .tournament(tournament)
                        .team(team)
                        .seed(seed)
                        .build()
        );
    }

    private void markEliminated(TournamentEntry entry) {
        entry.setLosses(2);
        entry.setStatus(TournamentEntryStatus.ELIMINATED);
        tournamentEntryRepository.save(entry);
    }

    private void createCompletedLosersEliminationMatch(Tournament tournament, int round, int matchIndex, TournamentEntry loser) {
        TournamentMatch match = TournamentMatch.builder()
                .tournament(tournament)
                .bracketType(TournamentBracketType.LOSERS)
                .roundNumber(round)
                .matchIndex(matchIndex)
                .state(TournamentMatchState.COMPLETE)
                .seriesLength(TournamentBracketBuilder.NORMAL_SERIES_LENGTH)
                .teamOneSeriesWins(0)
                .teamTwoSeriesWins(0)
                .loserEntry(loser)
                .build();
        tournamentMatchRepository.save(match);
    }

    private void assertSeedRank(List<TournamentRankingDto> rankings, int seed, int expectedRank) {
        TournamentRankingDto ranking = rankings.stream()
                .filter(row -> row.getSeed() == seed)
                .findFirst()
                .orElseThrow();
        assertEquals(expectedRank, ranking.getRank());
    }
}
