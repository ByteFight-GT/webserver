package com.example.botfightwebserver.player;

import com.example.botfightwebserver.PostgresIntegrationTest;
import com.example.botfightwebserver.submission.Submission;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDTOTest {

    private final LocalDateTime NOW = LocalDateTime.of(2006, 1, 2, 15, 30, 45);
    private final   LocalDateTime LATER = LocalDateTime.of(2006, 2, 1, 15, 30, 45);

    @Test
    void fromEntity() {
        Submission submission = Submission.builder().id(2L).build();
        Player player = Player.builder()
            .id(1L)
            .currentSubmission(submission)
            .glicko(1200.0)
            .email("tkwok123@gmail.com")
            .name("Tyler")
            .creationDateTime(NOW)
            .lastModifiedDate(LATER)
            .matchesPlayed(10)
            .numberWins(5)
            .numberLosses(3)
            .numberDraws(2)
            .build();
        PlayerDTO playerDTO = PlayerDTO.fromEntity(player);

        assertEquals(1L, playerDTO.getId());
        assertEquals(2L, playerDTO.getCurrentSubmissionDTO().id());
        assertEquals(1200.0, playerDTO.getGlicko());
        assertEquals("tkwok123@gmail.com", playerDTO.getEmail());
        assertEquals("Tyler", playerDTO.getName());
        assertEquals(NOW, playerDTO.getCreationDateTime());
        assertEquals(LATER, playerDTO.getLastModifiedDate());
        assertEquals(10, playerDTO.getMatchesPlayed());
        assertEquals(5, playerDTO.getNumberWins());
        assertEquals(3, playerDTO.getNumberLosses());
        assertEquals(2, playerDTO.getNumberDraws());
    }
}