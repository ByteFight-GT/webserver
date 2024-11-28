package com.example.botfightwebserver.team;

import com.example.botfightwebserver.PersistentTestBase;
import com.example.botfightwebserver.glicko.GlickoCalculator;
import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.submission.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class TeamServiceTest extends PersistentTestBase {

    @Autowired
    private TeamRepository teamRepository;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private GlickoCalculator glickoCalculator;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository, submissionService);
    }

    @Test
    void testGetTeams() {
        Submission submission1 = persistAndReturnEntity(new Submission());
        Submission submission2 = persistAndReturnEntity(new Submission());

        Team team1 = Team.builder()
            .currentSubmission(submission1)
            .glicko(1200.0)
            .name("Tyler")
            .matchesPlayed(10)
            .numberWins(5)
            .numberLosses(3)
            .numberDraws(2)
            .build();

        Team team2 = Team.builder()
            .currentSubmission(submission2)
            .glicko(1400.0)
            .name("Ben")
            .matchesPlayed(5)
            .numberWins(3)
            .numberLosses(2)
            .numberDraws(0)
            .build();

        persistEntity(team1);
        persistEntity(team2);

        List<Team> teams = teamService.getTeams();

        Team team1Persisted = teams.get(0);
        Team team2Persisted = teams.get(1);

        assertEquals(submission1.getId(), team1Persisted.getCurrentSubmission().getId());
        assertEquals(1200.0, team1Persisted.getGlicko());
        assertEquals("Tyler", team1Persisted.getName());
        assertEquals(10, team1Persisted.getMatchesPlayed());
        assertEquals(3, team1Persisted.getNumberLosses());
        assertEquals(2, team1Persisted.getNumberDraws());
        assertEquals(5, team1Persisted.getNumberWins());

        assertEquals(submission2.getId(), team2Persisted.getCurrentSubmission().getId());
        assertEquals(1400.0, team2Persisted.getGlicko());
        assertEquals("Ben", team2Persisted.getName());
        assertEquals(5, team2Persisted.getMatchesPlayed());
        assertEquals(3, team2Persisted.getNumberWins());
        assertEquals(2, team2Persisted.getNumberLosses());
        assertEquals(0, team2Persisted.getNumberDraws());
    }

    @Test
    void testGetTeams_none() {
        List<Team> teams = teamService.getTeams();
        assertEquals(0, teams.size());
    }

    @Test
    void testGetReferenceById() {
        Team expectedTeam = Team.builder()
            .glicko(1200.0)
            .name("Test Team")
            .matchesPlayed(0)
            .numberWins(0)
            .numberLosses(0)
            .numberDraws(0)
            .build();

        Team persistedTeam = persistAndReturnEntity(expectedTeam);
        Team retrievedTeam = teamService.getReferenceById(persistedTeam.getId());

        assertEquals(persistedTeam.getId(), retrievedTeam.getId());
        assertEquals(expectedTeam.getName(), retrievedTeam.getName());
        assertEquals(expectedTeam.getGlicko(), retrievedTeam.getGlicko());
        assertEquals(expectedTeam.getMatchesPlayed(), retrievedTeam.getMatchesPlayed());
        assertEquals(expectedTeam.getNumberWins(), retrievedTeam.getNumberWins());
        assertEquals(expectedTeam.getNumberLosses(), retrievedTeam.getNumberLosses());
        assertEquals(expectedTeam.getNumberDraws(), retrievedTeam.getNumberDraws());
    }

    @Test
    void testGetDTOById() {
        Submission submission = persistAndReturnEntity(new Submission());

        Team expectedTeam = Team.builder()
            .currentSubmission(submission)
            .glicko(1200.0)
            .name("Test Team")
            .matchesPlayed(5)
            .numberWins(3)
            .numberLosses(1)
            .numberDraws(1)
            .build();

        Team persistedTeam = persistAndReturnEntity(expectedTeam);

        TeamDTO teamDTO = teamService.getDTOById(persistedTeam.getId());

        assertEquals(persistedTeam.getId(), teamDTO.getId());
        assertEquals(expectedTeam.getName(), teamDTO.getName());
        assertEquals(expectedTeam.getGlicko(), teamDTO.getGlicko());
        assertEquals(expectedTeam.getMatchesPlayed(), teamDTO.getMatchesPlayed());
        assertEquals(expectedTeam.getNumberWins(), teamDTO.getNumberWins());
        assertEquals(expectedTeam.getNumberLosses(), teamDTO.getNumberLosses());
        assertEquals(expectedTeam.getNumberDraws(), teamDTO.getNumberDraws());
        assertEquals(submission.getId(), teamDTO.getCurrentSubmissionDTO().id());
    }

    @Test
    void testGetDTOById_WithNullSubmission() {
        Team expectedTeam = Team.builder()
            .currentSubmission(null)
            .glicko(1200.0)
            .name("Test Team")
            .matchesPlayed(0)
            .numberWins(0)
            .numberLosses(0)
            .numberDraws(0)
            .build();

        Team persistedTeam = persistAndReturnEntity(expectedTeam);

        TeamDTO teamDTO = teamService.getDTOById(persistedTeam.getId());

        assertEquals(persistedTeam.getId(), teamDTO.getId());
        assertEquals(expectedTeam.getName(), teamDTO.getName());
        assertEquals(expectedTeam.getGlicko(), teamDTO.getGlicko());
        assertEquals(expectedTeam.getMatchesPlayed(), teamDTO.getMatchesPlayed());
        assertEquals(expectedTeam.getNumberWins(), teamDTO.getNumberWins());
        assertEquals(expectedTeam.getNumberLosses(), teamDTO.getNumberLosses());
        assertEquals(expectedTeam.getNumberDraws(), teamDTO.getNumberDraws());
        assertEquals(null, teamDTO.getCurrentSubmissionDTO());
    }

    @Test
    void testCreateTeam_Success() {
        String name = "New Team";

        Team persistedTeam = teamService.createTeam(name);

        assertEquals(name, persistedTeam.getName());
        assertEquals(0, persistedTeam.getMatchesPlayed());
        assertEquals(0, persistedTeam.getNumberWins());
        assertEquals(0, persistedTeam.getNumberLosses());
        assertEquals(0, persistedTeam.getNumberDraws());
    }

    @Test
    void testCreateTeam_DuplicateName() {
        String name1 = "First Team";
        teamService.createTeam(name1);

        String name2 = "First Team";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamService.createTeam(name2)
        );

        assertEquals("Team with name " + name2 + " already exists", exception.getMessage());
        assertEquals(1, teamRepository.count());
    }


    @Test
    void testValidateTeams_Success() {
        Team team1 = persistAndReturnEntity(Team.builder()
            .name("Team 1")
            .build());

        Team team2 = persistAndReturnEntity(Team.builder()
            .name("Team 2")
            .build());

        assertDoesNotThrow(() ->
            teamService.validateTeams(team1.getId(), team2.getId())
        );
    }

    @Test
    void testValidateTeams_SameTeam() {
        Team team = persistAndReturnEntity(Team.builder()
            .name("Team 1")
            .build());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamService.validateTeams(team.getId(), team.getId())
        );

        assertEquals("Teams must be different", exception.getMessage());
    }

    @Test
    void testValidateTeams_TeamDoesNotExist() {
        Team team = persistAndReturnEntity(Team.builder()
            .name("Team 1")
            .build());

        Long nonExistentId = 99999L;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamService.validateTeams(nonExistentId, team.getId())
        );

        assertEquals("One or both teams do not exist", exception.getMessage());
    }


    @Test
    void testValidateTeams_NullIds() {
        assertThrows(IllegalArgumentException.class,
            () -> teamService.validateTeams(null, 1L));

        assertThrows(IllegalArgumentException.class,
            () -> teamService.validateTeams(1L, null));

        assertThrows(IllegalArgumentException.class,
            () -> teamService.validateTeams(null, null));
    }

    @Test
    void testUpdateAfterLadderMatch_Win() {
        Team initialTeam = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .glicko(1200.0)
            .matchesPlayed(5)
            .numberWins(2)
            .numberLosses(2)
            .numberDraws(1)
            .build());

        double glickoChange = 15.0;

        Team
            persistedTeam = teamService.updateAfterLadderMatch(initialTeam, glickoChange,0.0,0.0, true, false);

        assertEquals(1215.0, persistedTeam.getGlicko());
        assertEquals(6, persistedTeam.getMatchesPlayed());
        assertEquals(3, persistedTeam.getNumberWins());
        assertEquals(2, persistedTeam.getNumberLosses());
        assertEquals(1, persistedTeam.getNumberDraws());
    }

    @Test
    void testUpdateAfterLadderMatch_Loss() {
        Team initialTeam = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .glicko(1200.0)
            .matchesPlayed(5)
            .numberWins(2)
            .numberLosses(2)
            .numberDraws(1)
            .build());

        double glickoChange = -15.0;

        Team
            persistedTeam = teamService.updateAfterLadderMatch(initialTeam, glickoChange,0.0,0.0, false, false);

        assertEquals(1185.0, persistedTeam.getGlicko());
        assertEquals(6, persistedTeam.getMatchesPlayed());
        assertEquals(2, persistedTeam.getNumberWins());
        assertEquals(3, persistedTeam.getNumberLosses());
        assertEquals(1, persistedTeam.getNumberDraws());
    }

    @Test
    void testUpdateAfterLadderMatch_Draw() {
        Team initialTeam = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .glicko(1200.0)
            .matchesPlayed(5)
            .numberWins(2)
            .numberLosses(2)
            .numberDraws(1)
            .build());

        double glickoChange = 0.0;

        Team
            persistedTeam = teamService.updateAfterLadderMatch(initialTeam, glickoChange,0.0,0.0, false, true);

        assertEquals(1200.0, persistedTeam.getGlicko());
        assertEquals(6, persistedTeam.getMatchesPlayed());
        assertEquals(2, persistedTeam.getNumberWins());
        assertEquals(2, persistedTeam.getNumberLosses());
        assertEquals(2, persistedTeam.getNumberDraws());
    }

    @Test
    void testUpdateAfterLadderMatch_InvalidWinAndDraw() {
        Team initialTeam = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .glicko(1200.0)
            .matchesPlayed(5)
            .numberWins(2)
            .numberLosses(2)
            .numberDraws(1)
            .build());


        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamService.updateAfterLadderMatch(initialTeam, 15.0,0.0,0.0, true, true)
        );

        assertEquals("Result can't be a win and a draw", exception.getMessage());

        Team unchangedTeam = teamRepository.findById(initialTeam.getId()).get();
        assertEquals(1200.0, unchangedTeam.getGlicko());
        assertEquals(5, unchangedTeam.getMatchesPlayed());
        assertEquals(2, unchangedTeam.getNumberWins());
        assertEquals(2, unchangedTeam.getNumberLosses());
        assertEquals(1, unchangedTeam.getNumberDraws());
    }

    @Test
    void testSetCurrentSubmission_Success() {
        Team team = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .build());

        Long submissionId = 1L;
        Submission mockSubmission = new Submission();
        mockSubmission.setId(submissionId);

        when(submissionService.isSubmissionValid(submissionId)).thenReturn(true);
        when(submissionService.getSubmissionReferenceById(submissionId)).thenReturn(mockSubmission);

        assertDoesNotThrow(() ->
            teamService.setCurrentSubmission(team.getId(), submissionId)
        );

        Team updatedTeam = teamRepository.findById(team.getId()).get();
        assertEquals(submissionId, updatedTeam.getCurrentSubmission().getId());

        verify(submissionService).isSubmissionValid(submissionId);
        verify(submissionService).getSubmissionReferenceById(submissionId);
    }

    @Test
    void testSetCurrentSubmission_InvalidSubmission() {
        Team team = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .build());

        Long submissionId = 1L;

        when(submissionService.isSubmissionValid(submissionId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamService.setCurrentSubmission(team.getId(), submissionId)
        );

        assertEquals("Submission is not valid", exception.getMessage());

        Team unchangedTeam = teamRepository.findById(team.getId()).get();
        assertNull(unchangedTeam.getCurrentSubmission());

        verify(submissionService).isSubmissionValid(submissionId);
        verify(submissionService, never()).getSubmissionReferenceById(any());
    }

    @Test
    void testGetCurrentSubmission_WithSubmission() {
        Submission submission = persistAndReturnEntity(new Submission());

        Team team = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .currentSubmission(submission)
            .build());

        Optional<Submission> result = teamService.getCurrentSubmission(team.getId());

        assertTrue(result.isPresent());
        assertEquals(submission.getId(), result.get().getId());
    }

    @Test
    void testGetCurrentSubmission_NoSubmission() {
        Team team = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .currentSubmission(null)
            .build());

        Optional<Submission> result = teamService.getCurrentSubmission(team.getId());

        assertFalse(result.isPresent());
    }
    @Test
    void testPagination_ValidPageAndSize() {
        // Setup data: Create 10 teams
        List<Team> teams = IntStream.range(1, 11)
                .mapToObj(i -> persistAndReturnEntity(
                        Team.builder()
                                .name("Team " + i)
                                .glicko(1000.0 + i) // Add Glicko ratings for sorting
                                .build()))
                .collect(Collectors.toList());

        // Fetch page 0 with a size of 5
        List<Team> paginatedTeams = teamService.pagination(0, 5);

        assertEquals(5, paginatedTeams.size());
        assertEquals(teams.get(10 - 1).getId(), paginatedTeams.get(0).getId()); // First team on page 0 (highest Glicko)
        assertEquals(teams.get(6 - 1).getId(), paginatedTeams.get(4).getId()); // Last team on page 0
    }

    @Test
    void testPagination_PageSizeLargerThanTotalTeams() {
        // Setup data: Create 5 teams
        List<Team> teams = IntStream.range(1, 6)
                .mapToObj(i -> persistAndReturnEntity(
                        Team.builder()
                                .name("Team " + i)
                                .glicko(1000.0 + i) // Add Glicko ratings for sorting
                                .build()))
                .collect(Collectors.toList());

        // Fetch page 0 with a size of 10
        List<Team> paginatedTeams = teamService.pagination(0, 10);

        assertEquals(teams.size(), paginatedTeams.size());
        assertEquals(teams.get(5 - 1).getId(), paginatedTeams.get(0).getId()); // First team (highest Glicko)
        assertEquals(teams.get(1 - 1).getId(), paginatedTeams.get(4).getId()); // Last team
    }

    @Test
    void testPagination_NoTeams() {
        // Fetch page 0 with a size of 5 when there are no teams
        List<Team> paginatedTeams = teamService.pagination(0, 5);

        assertEquals(0, paginatedTeams.size());
    }

    @Test
    void testPagination_InvalidPageOrSize() {
        // Test invalid page size
        IllegalArgumentException exceptionForInvalidSize = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.pagination(0, 0)
        );
        assertEquals("Page size must be greater than 0", exceptionForInvalidSize.getMessage());

        // Test invalid page number
        IllegalArgumentException exceptionForInvalidPage = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.pagination(-1, 5)
        );
        assertEquals("Page index must be zero or greater", exceptionForInvalidPage.getMessage());
    }

    @Test
    void testPagination_SpecificRangeAndGlickoSorting() {
        // Setup data: Create 100 teams
        List<Team> teams = IntStream.range(1, 101)
                .mapToObj(i -> persistAndReturnEntity(
                        Team.builder()
                                .name("Team " + i)
                                .glicko(1000.0 + i) // Assign Glicko ratings for sorting
                                .build()))
                .collect(Collectors.toList());

        // Fetch teams 50–59 (page index 5, size 10)
        List<Team> paginatedTeams = teamService.pagination(5, 10);

        // Validate the size of the result
        assertEquals(10, paginatedTeams.size());

        // Validate that the teams are sorted by Glicko in descending order
        for (int i = 1; i < paginatedTeams.size(); i++) {
            assertTrue(
                    paginatedTeams.get(i - 1).getGlicko() >= paginatedTeams.get(i).getGlicko(),
                    "Teams are not sorted by Glicko in descending order"
            );
        }

        // Validate the specific teams retrieved
        assertEquals(1050.0, paginatedTeams.get(0).getGlicko());
        assertEquals(1041.0, paginatedTeams.get(9).getGlicko());
    }

    @Test
    void isExistById() {
        Team team = persistAndReturnEntity(Team.builder()
            .name("Test Team")
            .currentSubmission(null)
            .build());

        assertTrue(teamService.isExistById(team.getId()));
        assertFalse(teamService.isExistById(-1L));
    }

}