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

    @Mock
    private Team teamOne;
    @Mock
    private Team teamTwo;
    @Mock
    private Submission submissionOne;
    @Mock
    private Submission submissionTwo;
    @Mock
    private TeamDTO teamOneDTO;
    @Mock
    private TeamDTO teamTwoDTO;
    @Mock
    private SubmissionDTO submissionOneDTO;
    @Mock
    private SubmissionDTO submissionTwoDTO;

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

            teamDTOMock.when(() -> TeamDTO.fromEntity(teamOne)).thenReturn(teamOneDTO);
            teamDTOMock.when(() -> TeamDTO.fromEntity(teamTwo)).thenReturn(teamTwoDTO);
            submissionDTOMock.when(() -> SubmissionDTO.fromEntity(submissionOne)).thenReturn(submissionOneDTO);
            submissionDTOMock.when(() -> SubmissionDTO.fromEntity(submissionTwo)).thenReturn(submissionTwoDTO);

            GameMatchDTO dto = GameMatchDTO.fromEntity(gameMatch);

            assertNotNull(dto);
            assertEquals(1L, dto.getId());
            assertEquals(teamOneDTO, dto.getTeamOne());
            assertEquals(teamTwoDTO, dto.getTeamTwo());
            assertEquals(submissionOneDTO, dto.getSubmissionOne());
            assertEquals(submissionTwoDTO, dto.getSubmissionTwo());
            assertEquals(MATCH_STATUS.IN_PROGRESS, dto.getStatus());
            assertEquals(MATCH_REASON.LADDER, dto.getReason());
            assertEquals(createdAt, dto.getCreatedAt());
            assertEquals(processedAt, dto.getProcessedAt());
            assertEquals(2, dto.getTimesQueued());

            teamDTOMock.verify(() -> TeamDTO.fromEntity(teamOne));
            teamDTOMock.verify(() -> TeamDTO.fromEntity(teamTwo));
            submissionDTOMock.verify(() -> SubmissionDTO.fromEntity(submissionOne));
            submissionDTOMock.verify(() -> SubmissionDTO.fromEntity(submissionTwo));
        }
    }


}