package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamDTO;
import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.submission.SubmissionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameMatchDTOTest {


    private Team teamOne = Team.builder().name("Team One").build();

    private Team teamTwo = Team.builder().name("Team Two").build();

    private Submission submissionOne = Submission.builder().name("Submission One").build();

    private Submission submissionTwo = Submission.builder().name("Submission Two").build();


    private GameMatch gameMatch;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private final LocalDateTime processedAt = createdAt.plusHours(1);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        gameMatch = GameMatch.builder()
            .id(1L)
            .teamOne(teamOne)
            .teamTwo(teamTwo)
            .submissionOne(submissionOne)
            .submissionTwo(submissionTwo)
            .status(MATCH_STATUS.IN_PROGRESS)
            .reason(MATCH_REASON.LADDER)
            .createdAt(createdAt)
            .processedAt(processedAt)
            .timesQueued(2)
            .build();
    }

    @Test
    void testFromEntity() {
        try (MockedStatic<TeamDTO> teamDTOMock = mockStatic(TeamDTO.class);
             MockedStatic<SubmissionDTO> submissionDTOMock = mockStatic(SubmissionDTO.class)) {

            GameMatchDTO dto = GameMatchDTO.fromEntity(gameMatch);

            assertNotNull(dto);
            assertEquals(1L, dto.getId());
            assertEquals(teamOne.getName(), dto.getTeamOneName());
            assertEquals(teamTwo.getName(), dto.getTeamTwoName());
            assertEquals(submissionOne.getName(), dto.getSubmissionOneName());
            assertEquals(submissionTwo.getName(), dto.getSubmissionTwoName());
            assertEquals(MATCH_STATUS.IN_PROGRESS, dto.getStatus());
            assertEquals(MATCH_REASON.LADDER, dto.getReason());
            assertEquals(createdAt, dto.getCreatedAt());
            assertEquals(processedAt, dto.getProcessedAt());
            assertEquals(2, dto.getTimesQueued());
        }
    }


}