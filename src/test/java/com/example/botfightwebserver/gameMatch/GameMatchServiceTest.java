package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.gameMatchLogs.GameMatchLogService;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
import com.example.botfightwebserver.submission.STORAGE_SOURCE;
import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.submission.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameMatchServiceTest {

    @Mock
    private GameMatchRepository gameMatchRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private SubmissionService submissionService;
    @Mock
    private RabbitMQService rabbitMQService;
    @Mock
    private GameMatchLogService gameMatchLogService;

    private Clock fixedClock;
    private GameMatchService gameMatchService;

    @BeforeEach
    void setUp() {
        // Set up a fixed clock for testing
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneId.systemDefault());
        gameMatchService = new GameMatchService(
                gameMatchRepository,
            teamService,
                submissionService,
                rabbitMQService,
                gameMatchLogService,
                fixedClock
        );
    }

    @Test
    void createMatch_ShouldCreateNewGameMatch() {
        // Arrange
        Long team1Id = 1L;
        Long team2Id = 2L;
        Long submission1Id = 1L;
        Long submission2Id = 2L;
        String map = "testMap";

        Team team1 = new Team();
        Team team2 = new Team();
        Submission submission1 = new Submission();
        Submission submission2 = new Submission();

        when(teamService.getReferenceById(team1Id)).thenReturn(team1);
        when(teamService.getReferenceById(team2Id)).thenReturn(team2);
        when(submissionService.getSubmissionReferenceById(submission1Id)).thenReturn(submission1);
        when(submissionService.getSubmissionReferenceById(submission2Id)).thenReturn(submission2);
        when(gameMatchRepository.save(any(GameMatch.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        GameMatch result = gameMatchService.createMatch(team1Id, team2Id, submission1Id, submission2Id, MATCH_REASON.SCRIMMAGE, map);

        // Assert
        assertNotNull(result);
        assertEquals(MATCH_STATUS.WAITING, result.getStatus());
        assertEquals(map, result.getMap());
        assertEquals(MATCH_REASON.SCRIMMAGE, result.getReason());
        assertEquals(1, result.getTimesQueued());
        assertEquals(LocalDateTime.now(fixedClock), result.getQueuedAt());
        verify(teamService).validateTeams(team1Id, team2Id);
        verify(submissionService).validateSubmissions(submission1Id, submission2Id);
    }

    @Test
    void submitGameMatch_ShouldCreateAndEnqueueMatch() {
        // Arrange
        Long team1Id = 1L;
        Long team2Id = 2L;
        Long submission1Id = 1L;
        Long submission2Id = 2L;
        String map = "testMap";

        // Create actual objects for GameMatch and Submissions
        Submission submission1 = new Submission();
        submission1.setId(submission1Id);
        submission1.setStoragePath("path/to/submission1");
        submission1.setSource(STORAGE_SOURCE.LOCAL);

        Submission submission2 = new Submission();
        submission2.setId(submission2Id);
        submission2.setStoragePath("path/to/submission2");
        submission2.setSource(STORAGE_SOURCE.LOCAL);

        GameMatch gameMatch = new GameMatch();
        gameMatch.setId(1L);
        gameMatch.setSubmissionOne(submission1);
        gameMatch.setSubmissionTwo(submission2);
        gameMatch.setMap(map);

        // Mock repository behavior
        when(gameMatchRepository.save(any(GameMatch.class))).thenReturn(gameMatch);

        // Act
//        GameMatchJob result = gameMatchService.submitGameMatch(
//                team1Id, team2Id, submission1Id, submission2Id, MATCH_REASON.SCRIMMAGE, map);

        // Assert
//        assertNotNull(result);  // Ensure the result is not null
//        assertEquals(1L, result.gameMatchId());  // Verify match ID
//        assertEquals("path/to/submission1", result.Submission1Path());
//        assertEquals("path/to/submission2", result.Submission2Path());
//        assertEquals(STORAGE_SOURCE.LOCAL, result.source1());
//        assertEquals(STORAGE_SOURCE.LOCAL, result.source2());
//
//        // Verify repository save and RabbitMQ enqueue
//        verify(gameMatchRepository).save(any(GameMatch.class));
//        verify(rabbitMQService).enqueueGameMatchJob(any(GameMatchJob.class));
    }

    @Test
    void setGameMatchStatus_ShouldUpdateStatus() {
        // Arrange
        Long gameMatchId = 1L;
        GameMatch gameMatch = new GameMatch();
        gameMatch.setStatus(MATCH_STATUS.WAITING);

        when(gameMatchRepository.findById(gameMatchId)).thenReturn(Optional.of(gameMatch));
        when(gameMatchRepository.save(any(GameMatch.class))).thenReturn(gameMatch);

        // Act
        gameMatchService.setGameMatchStatus(gameMatchId, MATCH_STATUS.WAITING);

        // Assert
        assertEquals(MATCH_STATUS.WAITING, gameMatch.getStatus());
        verify(gameMatchRepository).save(gameMatch);
    }

    @Test
    void getStaleWaitingMatches_ShouldReturnMatchesBeyondThreshold() {
        // Arrange
        LocalDateTime thresholdTime = LocalDateTime.now(fixedClock)
                .minusMinutes(GameMatchService.STALE_THRESHOLD_MINUTES);
        List<GameMatch> staleMatches = Arrays.asList(new GameMatch(), new GameMatch());

        when(gameMatchRepository.findByStatusAndQueuedAtBefore(
                MATCH_STATUS.WAITING, thresholdTime))
                .thenReturn(staleMatches);

        // Act
        List<GameMatch> result = gameMatchService.getStaleWaitingMatches();

        // Assert
        assertEquals(staleMatches.size(), result.size());
        verify(gameMatchRepository).findByStatusAndQueuedAtBefore(
                MATCH_STATUS.WAITING, thresholdTime);
    }

    @Test
    void rescheduleMatch_ShouldThrowException_WhenMaxRetriesExceeded() {
        // Arrange
        GameMatch gameMatch = new GameMatch();
        gameMatch.setId(1L);
        gameMatch.setTimesQueued(3);

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class,
                () -> gameMatchService.rescheduleMatch(gameMatch));
        assertTrue(exception.getMessage().contains("exceeded maximum retry attempts"));
    }

    @Test
    void rescheduleMatch_ShouldRescheduleSuccessfully() {
        // Arrange
        GameMatch gameMatch = new GameMatch();  // Use real object instead of mock
        gameMatch.setId(1L);
        gameMatch.setTimesQueued(1);  // Initially set timesQueued to 1

        // Mock the submissions
        Submission submission1 = mock(Submission.class);
        Submission submission2 = mock(Submission.class);

        // Set up the behavior for submission mocks
        when(submission1.getStoragePath()).thenReturn("path/to/submission1");
        when(submission2.getStoragePath()).thenReturn("path/to/submission2");

        when(submission1.getSource()).thenReturn(STORAGE_SOURCE.LOCAL);
        when(submission2.getSource()).thenReturn(STORAGE_SOURCE.LOCAL);

        // Set up gameMatch to return mocked submissions
        gameMatch.setSubmissionOne(submission1); // Set real submissions
        gameMatch.setSubmissionTwo(submission2);

        // Mock repository save method
        when(gameMatchRepository.save(any(GameMatch.class))).thenReturn(gameMatch);

        // Mock the void method for rabbitMQService.enqueueGameMatchJob
        doNothing().when(rabbitMQService).enqueueGameMatchJob(any(GameMatchJob.class));

        // Act
        GameMatchJob result = gameMatchService.rescheduleMatch(gameMatch);

        // Assert
        assertNotNull(result);
        assertEquals(2, gameMatch.getTimesQueued());  // Check that timesQueued has been incremented
        assertEquals(MATCH_STATUS.WAITING, gameMatch.getStatus());  // Check the status
        assertNotNull(gameMatch.getQueuedAt());  // Ensure the queuedAt time is set
        verify(rabbitMQService).enqueueGameMatchJob(any(GameMatchJob.class));  // Verify the job was enqueued
        verify(gameMatchRepository).save(gameMatch);  // Verify save was called
    }
}