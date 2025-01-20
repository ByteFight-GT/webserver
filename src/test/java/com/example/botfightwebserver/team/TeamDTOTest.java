package com.example.botfightwebserver.team;

import com.example.botfightwebserver.submission.Submission;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamDTOTest {

    private final LocalDateTime NOW = LocalDateTime.of(2006, 1, 2, 15, 30, 45);
    private final   LocalDateTime LATER = LocalDateTime.of(2006, 2, 1, 15, 30, 45);

    @Test
    void fromEntity() {
        Submission submission = Submission.builder().id(2L).build();
        Team team = Team.builder()
            .id(1L)
            .currentSubmission(submission)
            .glicko(1200.0)
            .name("Tyler Team")
            .creationDateTime(NOW)
            .lastModifiedDate(LATER)
            .matchesPlayed(10)
            .numberWins(5)
            .numberLosses(3)
            .numberDraws(2)
            .quote("some quote")
            .build();
        TeamDTO teamDTO = TeamDTO.fromEntity(team);

        assertEquals(1L, teamDTO.getId());
        assertEquals(2L, teamDTO.getCurrentSubmissionDTO().id());
        assertEquals(1200.0, teamDTO.getGlicko());
        assertEquals("Tyler Team", teamDTO.getName());
        assertEquals(NOW, teamDTO.getCreationDateTime());
        assertEquals(LATER, teamDTO.getLastModifiedDate());
        assertEquals(10, teamDTO.getMatchesPlayed());
        assertEquals(5, teamDTO.getNumberWins());
        assertEquals(3, teamDTO.getNumberLosses());
        assertEquals(2, teamDTO.getNumberDraws());
        assertEquals("some quote", teamDTO.getQuote());
    }
}