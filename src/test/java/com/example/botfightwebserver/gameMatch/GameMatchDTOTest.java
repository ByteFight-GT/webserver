package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.dto.GameMatchDto;
import com.example.botfightwebserver.gameMatch.domain.MatchReason;
import com.example.botfightwebserver.gameMatch.domain.MatchStatus;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.submission.domain.Submission;
import com.example.botfightwebserver.submission.domain.SubmissionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
            .status(MatchStatus.IN_PROGRESS)
            .reason(MatchReason.LADDER)
            .createdAt(createdAt)
            .processedAt(processedAt)
            .timesQueued(2)
            .build();
    }

    @Test
    void testFromEntity() {
        try (MockedStatic<TeamDTO> teamDTOMock = mockStatic(TeamDTO.class);
             MockedStatic<SubmissionDTO> submissionDTOMock = mockStatic(SubmissionDTO.class)) {

            GameMatchDto dto = GameMatchDto.fromEntity(gameMatch);

            assertNotNull(dto);
            assertEquals(1L, dto.getId());
            assertEquals(teamOne.getName(), dto.getTeamOneName());
            assertEquals(teamTwo.getName(), dto.getTeamTwoName());
            assertEquals(submissionOne.getName(), dto.getSubmissionOneName());
            assertEquals(submissionTwo.getName(), dto.getSubmissionTwoName());
            assertEquals(MatchStatus.IN_PROGRESS, dto.getStatus());
            assertEquals(MatchReason.LADDER, dto.getReason());
            assertEquals(createdAt, dto.getCreatedAt());
            assertEquals(processedAt, dto.getProcessedAt());
            assertEquals(2, dto.getTimesQueued());
        }
    }


}