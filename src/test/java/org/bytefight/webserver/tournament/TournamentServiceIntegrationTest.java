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
import org.bytefight.webserver.tournament.application.TournamentBracketBuilder;
import org.bytefight.webserver.tournament.application.TournamentService;
import org.bytefight.webserver.tournament.domain.CreateTournamentRequest;
import org.bytefight.webserver.tournament.domain.EnrollTeamsRequest;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentDto;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
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
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentEntryRepository tournamentEntryRepository;

    @Autowired
    private TournamentMatchRepository tournamentMatchRepository;

    @Test
    void createTournamentIsCompetitionScoped() {
        Competition competition = createCompetition("comp-create", true);
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("Winter Cup");
        request.setMaxTeams(16);

        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);
        Tournament tournament = tournamentRepository.findByUuidAndCompetition(UUID.fromString(dto.getUuid()), competition).orElseThrow();

        assertEquals(competition.getId(), tournament.getCompetition().getId());
        assertEquals(TournamentStatus.DRAFT, tournament.getStatus());
    }

    @Test
    void enrollTeamsRequiresActiveCompetition() {
        Competition competition = createCompetition("comp-inactive", false);
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("Inactive Cup");
        request.setMaxTeams(8);
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);

        assertThrows(IllegalArgumentException.class, () ->
                tournamentService.enrollTeams(competition.getSlug(), dto.getUuid(), new EnrollTeamsRequest()));
    }

    @Test
    void enrollTeamsRejectsTeamWithoutSubmission() {
        Competition competition = createCompetition("comp-nosub", true);
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("NoSub Cup");
        request.setMaxTeams(8);
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);

        Team team = createTeam(competition, "Alpha", false);
        EnrollTeamsRequest enrollTeamsRequest = new EnrollTeamsRequest();
        enrollTeamsRequest.setTeamUuids(List.of(team.getUuid().toString()));

        assertThrows(IllegalArgumentException.class, () ->
                tournamentService.enrollTeams(competition.getSlug(), dto.getUuid(), enrollTeamsRequest));
    }

    @Test
    void enrollTeamsRespectsMaxTeams() {
        Competition competition = createCompetition("comp-max", true);
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("Limited Cup");
        request.setMaxTeams(1);
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);

        Team teamA = createTeam(competition, "Alpha", true);
        Team teamB = createTeam(competition, "Beta", true);
        EnrollTeamsRequest enrollTeamsRequest = new EnrollTeamsRequest();
        enrollTeamsRequest.setTeamUuids(List.of(teamA.getUuid().toString(), teamB.getUuid().toString()));

        assertThrows(IllegalArgumentException.class, () ->
                tournamentService.enrollTeams(competition.getSlug(), dto.getUuid(), enrollTeamsRequest));
    }

    @Test
    void startTournamentRequiresAtLeastTwoTeams() {
        Competition competition = createCompetition("comp-min", true);
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("Solo Cup");
        request.setMaxTeams(8);
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);

        Team team = createTeam(competition, "Alpha", true);
        EnrollTeamsRequest enrollTeamsRequest = new EnrollTeamsRequest();
        enrollTeamsRequest.setTeamUuids(List.of(team.getUuid().toString()));
        tournamentService.enrollTeams(competition.getSlug(), dto.getUuid(), enrollTeamsRequest);

        assertThrows(IllegalArgumentException.class, () ->
                tournamentService.startTournament(competition.getSlug(), dto.getUuid()));
    }

    @Test
    void startTournamentBuildsBracketAndQueuesMatches() {
        Competition competition = createCompetition("comp-start", true);
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("Bracket Cup");
        request.setMaxTeams(8);
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);

        Team teamA = createTeam(competition, "Alpha", true);
        Team teamB = createTeam(competition, "Beta", true);
        Team teamC = createTeam(competition, "Gamma", true);
        Team teamD = createTeam(competition, "Delta", true);

        EnrollTeamsRequest enrollTeamsRequest = new EnrollTeamsRequest();
        enrollTeamsRequest.setTeamUuids(List.of(
                teamA.getUuid().toString(),
                teamB.getUuid().toString(),
                teamC.getUuid().toString(),
                teamD.getUuid().toString()
        ));
        tournamentService.enrollTeams(competition.getSlug(), dto.getUuid(), enrollTeamsRequest);
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
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("Bye Cup");
        request.setMaxTeams(8);
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);

        Team teamA = createTeam(competition, "Alpha", true);
        Team teamB = createTeam(competition, "Beta", true);
        Team teamC = createTeam(competition, "Gamma", true);

        EnrollTeamsRequest enrollTeamsRequest = new EnrollTeamsRequest();
        enrollTeamsRequest.setTeamUuids(List.of(
                teamA.getUuid().toString(),
                teamB.getUuid().toString(),
                teamC.getUuid().toString()
        ));
        tournamentService.enrollTeams(competition.getSlug(), dto.getUuid(), enrollTeamsRequest);
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
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("Series Cup");
        request.setMaxTeams(8);
        TournamentDto dto = tournamentService.createTournament(competition.getSlug(), request);

        Team teamA = createTeam(competition, "Alpha", true);
        Team teamB = createTeam(competition, "Beta", true);
        Team teamC = createTeam(competition, "Gamma", true);
        Team teamD = createTeam(competition, "Delta", true);

        EnrollTeamsRequest enrollTeamsRequest = new EnrollTeamsRequest();
        enrollTeamsRequest.setTeamUuids(List.of(
                teamA.getUuid().toString(),
                teamB.getUuid().toString(),
                teamC.getUuid().toString(),
                teamD.getUuid().toString()
        ));
        tournamentService.enrollTeams(competition.getSlug(), dto.getUuid(), enrollTeamsRequest);
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

    // ── Helper methods ──────────────────────────────────────────────────────

    private Competition createCompetition(String slug, boolean active) {
        Competition competition = new Competition();
        competition.setSlug(slug + "-" + UUID.randomUUID());
        competition.setName("Competition " + slug);
        competition.setActive(active);
        competition.setWhitelisted(false);
        competition.setMaxPlayersPerTeam(2);
        return competitionRepository.save(competition);
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
}
