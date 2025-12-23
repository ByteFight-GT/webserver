package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.MatchReason;
import com.example.botfightwebserver.gameMatch.domain.MatchStatus;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.submission.domain.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class GameMatchTest {
    private GameMatch gameMatch;
    private Team teamOne;
    private Team teamTwo;
    private Submission submissionOne;
    private Submission submissionTwo;
    private Clock fixedClock;

    private final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        teamOne = new Team();
        teamOne.setId(1L);

        teamTwo = new Team();
        teamTwo.setId(2L);

        submissionOne = new Submission();
        submissionOne.setId(1L);

        submissionTwo = new Submission();
        submissionTwo.setId(2L);

        gameMatch = new GameMatch();
        gameMatch.setTeamOne(teamOne);
        gameMatch.setTeamTwo(teamTwo);
        gameMatch.setSubmissionOne(submissionOne);
        gameMatch.setSubmissionTwo(submissionTwo);
        gameMatch.setMap("test_map");

        fixedClock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        GameMatch.setClock(fixedClock);
    }

    @Test
    void testPrePersist() {
        gameMatch.onCreate();

        assertEquals(MatchStatus.WAITING, gameMatch.getStatus());
        assertEquals(MatchReason.UNKNOWN, gameMatch.getReason());
        assertEquals(NOW, gameMatch.getCreatedAt());
    }

    @Test
    void testCustomStatusAndReasonAreNotOverwritten() {
        gameMatch.setStatus(MatchStatus.IN_PROGRESS);
        gameMatch.setReason(MatchReason.LADDER);

        gameMatch.onCreate();

        assertEquals(MatchStatus.IN_PROGRESS, gameMatch.getStatus());
        assertEquals(MatchReason.LADDER, gameMatch.getReason());
    }


    @Test
    void testNoArgsConstructor() {
        GameMatch match = new GameMatch();

        assertNull(match.getId());
        assertNull(match.getTeamOne());
        assertNull(match.getTeamTwo());
        assertNull(match.getSubmissionOne());
        assertNull(match.getSubmissionTwo());
        assertNull(match.getStatus());
        assertNull(match.getReason());
        assertNull(match.getCreatedAt());
        assertNull(match.getProcessedAt());
        assertNull(match.getMap());
    }

    @Test
    void testSettersAndGetters() {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        LocalDateTime processed = LocalDateTime.now(fixedClock).plusMinutes(5);

        gameMatch.setId(1L);
        gameMatch.setStatus(MatchStatus.IN_PROGRESS);
        gameMatch.setReason(MatchReason.LADDER);
        gameMatch.setCreatedAt(now);
        gameMatch.setProcessedAt(processed);
        gameMatch.setMap("new_map");

        assertEquals(1L, gameMatch.getId());
        assertEquals(teamOne, gameMatch.getTeamOne());
        assertEquals(teamTwo, gameMatch.getTeamTwo());
        assertEquals(submissionOne, gameMatch.getSubmissionOne());
        assertEquals(submissionTwo, gameMatch.getSubmissionTwo());
        assertEquals(MatchStatus.IN_PROGRESS, gameMatch.getStatus());
        assertEquals(MatchReason.LADDER, gameMatch.getReason());
        assertEquals(now, gameMatch.getCreatedAt());
        assertEquals(processed, gameMatch.getProcessedAt());
        assertEquals("new_map", gameMatch.getMap());
    }
}