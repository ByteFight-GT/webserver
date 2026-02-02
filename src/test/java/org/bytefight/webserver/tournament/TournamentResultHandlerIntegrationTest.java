package org.bytefight.webserver.tournament;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.tournament.application.TournamentResultHandler;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentEntryStatus;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.bytefight.webserver.tournament.domain.TournamentStatus;
import org.bytefight.webserver.tournament.infra.TournamentEntryRepository;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import org.bytefight.webserver.tournament.infra.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TournamentResultHandlerIntegrationTest extends FullStackIntegrationTestBase {

    @Autowired
    private TournamentResultHandler tournamentResultHandler;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentEntryRepository tournamentEntryRepository;

    @Autowired
    private TournamentMatchRepository tournamentMatchRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private GameMatchService gameMatchService;

    @Test
    void handleTournamentResultIncrementsLossesAndEliminatesOnSecondLoss() {
        Competition competition = createCompetition("comp-result", true);
        Tournament tournament = createTournament(competition);

        Team teamA = createTeamWithSubmission(competition, "Alpha");
        Team teamB = createTeamWithSubmission(competition, "Beta");

        TournamentEntry entryA = createEntry(tournament, teamA, 1);
        TournamentEntry entryB = createEntry(tournament, teamB, 2);

        TournamentMatch match1 = createMatch(tournament, entryA, entryB);
        GameMatch gameMatch1 = createGameMatch(teamA, teamB);
        match1.setGameMatch(gameMatch1);
        match1.setState(TournamentMatchState.QUEUED);
        tournamentMatchRepository.save(match1);

        tournamentResultHandler.handleTournamentResult(gameMatch1, MatchStatus.team_a_win);

        TournamentEntry refreshedLoser = tournamentEntryRepository.findById(entryB.getId()).orElseThrow();
        TournamentMatch refreshedMatch = tournamentMatchRepository.findById(match1.getId()).orElseThrow();

        assertEquals(1, refreshedLoser.getLosses());
        assertEquals(TournamentEntryStatus.ACTIVE, refreshedLoser.getStatus());
        assertEquals(TournamentMatchState.COMPLETE, refreshedMatch.getState());

        TournamentMatch match2 = createMatch(tournament, entryA, refreshedLoser);
        GameMatch gameMatch2 = createGameMatch(teamA, teamB);
        match2.setGameMatch(gameMatch2);
        match2.setState(TournamentMatchState.QUEUED);
        tournamentMatchRepository.save(match2);

        tournamentResultHandler.handleTournamentResult(gameMatch2, MatchStatus.team_a_win);

        TournamentEntry eliminated = tournamentEntryRepository.findById(entryB.getId()).orElseThrow();
        assertEquals(2, eliminated.getLosses());
        assertEquals(TournamentEntryStatus.ELIMINATED, eliminated.getStatus());
        assertNotNull(eliminated.getEliminatedAt());
    }

    @Test
    void handleTournamentResultDrawRequeuesMatch() {
        Competition competition = createCompetition("comp-draw", true);
        Tournament tournament = createTournament(competition);

        Team teamA = createTeamWithSubmission(competition, "Alpha");
        Team teamB = createTeamWithSubmission(competition, "Beta");

        TournamentEntry entryA = createEntry(tournament, teamA, 1);
        TournamentEntry entryB = createEntry(tournament, teamB, 2);

        TournamentMatch match = createMatch(tournament, entryA, entryB);
        GameMatch gameMatch = createGameMatch(teamA, teamB);
        match.setGameMatch(gameMatch);
        match.setState(TournamentMatchState.QUEUED);
        tournamentMatchRepository.save(match);

        tournamentResultHandler.handleTournamentResult(gameMatch, MatchStatus.draw);

        TournamentMatch refreshed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
        assertEquals(TournamentMatchState.QUEUED, refreshed.getState());
        assertNotNull(refreshed.getGameMatch());
    }

    private Competition createCompetition(String slug, boolean active) {
        Competition competition = new Competition();
        competition.setSlug(slug + "-" + UUID.randomUUID());
        competition.setName("Competition " + slug);
        competition.setActive(active);
        competition.setWhitelisted(false);
        competition.setMaxPlayersPerTeam(2);
        return competitionRepository.save(competition);
    }

    private Tournament createTournament(Competition competition) {
        Tournament tournament = Tournament.builder()
                .competition(competition)
                .name("Tournament")
                .status(TournamentStatus.IN_PROGRESS)
                .build();
        return tournamentRepository.save(tournament);
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

    private TournamentEntry createEntry(Tournament tournament, Team team, int seed) {
        TournamentEntry entry = TournamentEntry.builder()
                .tournament(tournament)
                .team(team)
                .seed(seed)
                .build();
        return tournamentEntryRepository.save(entry);
    }

    private TournamentMatch createMatch(Tournament tournament, TournamentEntry teamOne, TournamentEntry teamTwo) {
        TournamentMatch match = TournamentMatch.builder()
                .tournament(tournament)
                .bracketType(TournamentBracketType.WINNERS)
                .roundNumber(1)
                .matchIndex(1)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .state(TournamentMatchState.PENDING)
                .build();
        return tournamentMatchRepository.save(match);
    }

    private GameMatch createGameMatch(Team teamA, Team teamB) {
        return gameMatchService.createMatch(
                null,
                teamA,
                teamB,
                teamA.getCurrentSubmission(),
                teamB.getCurrentSubmission(),
                "tournament",
                MatchReason.tournament,
                null
        );
    }
}
