package com.example.botfightwebserver.team;

import com.example.botfightwebserver.PersistentTestBase;
import com.example.botfightwebserver.submission.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class TeamTest extends PersistentTestBase {

    private Submission testSubmission;
    private Clock fixedClock;

    private final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    @BeforeEach
    void setup() {
        fixedClock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        Team.setClock(fixedClock);
        testSubmission = persistAndReturnEntity(new Submission());
    }

    @Test
    void testBuilderWithAllFields() {
        LocalDateTime now = LocalDateTime.now();

        Team team = Team.builder()
            .id(1L)
            .name("John Doe")
            .creationDateTime(now)
            .lastModifiedDate(now)
            .glicko(1500.0)
            .matchesPlayed(10)
            .numberWins(5)
            .numberLosses(3)
            .numberDraws(2)
            .currentSubmission(testSubmission)
            .quote("ByteFight ftw")
            .build();

        assertEquals(1L, team.getId());
        assertEquals("John Doe", team.getName());
        assertEquals(now, team.getCreationDateTime());
        assertEquals(now, team.getLastModifiedDate());
        assertEquals(1500.0, team.getGlicko());
        assertEquals(10, team.getMatchesPlayed());
        assertEquals(5, team.getNumberWins());
        assertEquals(3, team.getNumberLosses());
        assertEquals(2, team.getNumberDraws());
        assertEquals("ByteFight ftw", team.getQuote());
        assertEquals(testSubmission, team.getCurrentSubmission());
    }

    @Test
    void testDefaultValues() {
        Team team = Team.builder()
            .name("Jane Doe")
            .currentSubmission(testSubmission)
            .build();

        assertEquals(1500.0, team.getGlicko());
        assertEquals(0, team.getMatchesPlayed());
        assertEquals(0, team.getNumberWins());
        assertEquals(0, team.getNumberLosses());
        assertEquals(0, team.getNumberDraws());
        assertEquals("Welcome to ByteFight!", team.getQuote());
    }

    @Test
    void testPrePersist() {
        Team team = Team.builder()
            .name("Test Team")
            .currentSubmission(testSubmission)
            .build();

        persistEntity(team);

        assertEquals(NOW, team.getCreationDateTime());
        assertEquals(NOW, team.getLastModifiedDate());
    }

    @Test
    void testCurrentSubmissionNull() {
        Team team = Team.builder()
            .name("Test Team")
            .build();
       Team persistedTeam = persistAndReturnEntity(team);

       assertEquals("Test Team", persistedTeam.getName());
       assertNotNull(persistedTeam.getId());
    }

    @Test
    void teamUpdateSetsModifiedDate() {
        Team team = Team.builder()
            .name("Test Team")
            .currentSubmission(testSubmission)
            .build();

        team.onCreate();

        team.setMatchesPlayed(1);
        team.setNumberWins(1);

        LocalDateTime dayAfter = NOW.plusDays(1);
        Clock newClock = Clock.fixed(dayAfter.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        Team.setClock(newClock);

        team.onUpdate();

        assertNotNull(team.getCreationDateTime());
        assertNotNull(team.getLastModifiedDate());
        assertEquals(NOW, team.getCreationDateTime());
        assertEquals(dayAfter, team.getLastModifiedDate());
    }
}