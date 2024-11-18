package com.example.botfightwebserver.player;

import com.example.botfightwebserver.PersistentTestBase;
import com.example.botfightwebserver.submission.Submission;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class PlayerTest extends PersistentTestBase {

    private Submission testSubmission;
    private Clock fixedClock;

    private final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    @BeforeEach
    void setup() {
        fixedClock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        Player.setClock(fixedClock);
        testSubmission = persistAndReturnEntity(new Submission());
    }

    @Test
    void testBuilderWithAllFields() {
        LocalDateTime now = LocalDateTime.now();

        Player player = Player.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .creationDateTime(now)
            .lastModifiedDate(now)
            .elo(1500.0)
            .matchesPlayed(10)
            .numberWins(5)
            .numberLosses(3)
            .numberDraws(2)
            .currentSubmission(testSubmission)
            .build();

        assertEquals(1L, player.getId());
        assertEquals("John Doe", player.getName());
        assertEquals("john@example.com", player.getEmail());
        assertEquals(now, player.getCreationDateTime());
        assertEquals(now, player.getLastModifiedDate());
        assertEquals(1500.0, player.getElo());
        assertEquals(10, player.getMatchesPlayed());
        assertEquals(5, player.getNumberWins());
        assertEquals(3, player.getNumberLosses());
        assertEquals(2, player.getNumberDraws());
        assertEquals(testSubmission, player.getCurrentSubmission());
    }

    @Test
    void testDefaultValues() {
        Player player = Player.builder()
            .name("Jane Doe")
            .email("jane@example.com")
            .currentSubmission(testSubmission)
            .build();

        assertEquals(1200.0, player.getElo());
        assertEquals(0, player.getMatchesPlayed());
        assertEquals(0, player.getNumberWins());
        assertEquals(0, player.getNumberLosses());
        assertEquals(0, player.getNumberDraws());
    }

    @Test
    void testPrePersist() {
        Player player = Player.builder()
            .name("Test Player")
            .email("test@example.com")
            .currentSubmission(testSubmission)
            .build();

        persistEntity(player);

        assertEquals(NOW, player.getCreationDateTime());
        assertEquals(NOW, player.getLastModifiedDate());
    }

    @Test
    void testCurrentSubmissionNull() {
        Player player = Player.builder()
            .name("Test Player")
            .email("test@example.com")
            .build();
       Player persistedPlayer = persistAndReturnEntity(player);

       assertEquals("Test Player", persistedPlayer.getName());
       assertEquals("test@example.com", persistedPlayer.getEmail());
       assertNotNull(persistedPlayer.getId());
    }

    @Test
    void testInvalidEmail() {
        Player player = Player.builder()
            .name("Test Player")
            .email("invalidEmail")
            .build();

        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> {
            persistAndReturnEntity(player);
        });

        assertEquals("must be a well-formed email address",
            exception.getConstraintViolations().iterator().next().getMessage());
    }

    @Test
    void playerUpdateSetsModifiedDate() {
        Player player = Player.builder()
            .name("Test Player")
            .email("test@example.com")
            .currentSubmission(testSubmission)
            .build();

        player.onCreate();

        player.setMatchesPlayed(1);
        player.setNumberWins(1);

        LocalDateTime dayAfter = NOW.plusDays(1);
        Clock newClock = Clock.fixed(dayAfter.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        Player.setClock(newClock);

        player.onUpdate();

        assertNotNull(player.getCreationDateTime());
        assertNotNull(player.getLastModifiedDate());
        assertEquals(NOW, player.getCreationDateTime());
        assertEquals(dayAfter, player.getLastModifiedDate());
    }
}