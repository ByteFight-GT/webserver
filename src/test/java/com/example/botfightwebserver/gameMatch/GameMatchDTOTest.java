package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.player.PlayerDTO;
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
    private Player playerOne;
    @Mock
    private Player playerTwo;
    @Mock
    private Submission submissionOne;
    @Mock
    private Submission submissionTwo;
    @Mock
    private PlayerDTO playerOneDTO;
    @Mock
    private PlayerDTO playerTwoDTO;
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
            .playerOne(playerOne)
            .playerTwo(playerTwo)
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
        try (MockedStatic<PlayerDTO> playerDTOMock = mockStatic(PlayerDTO.class);
             MockedStatic<SubmissionDTO> submissionDTOMock = mockStatic(SubmissionDTO.class)) {

            playerDTOMock.when(() -> PlayerDTO.fromEntity(playerOne)).thenReturn(playerOneDTO);
            playerDTOMock.when(() -> PlayerDTO.fromEntity(playerTwo)).thenReturn(playerTwoDTO);
            submissionDTOMock.when(() -> SubmissionDTO.fromEntity(submissionOne)).thenReturn(submissionOneDTO);
            submissionDTOMock.when(() -> SubmissionDTO.fromEntity(submissionTwo)).thenReturn(submissionTwoDTO);

            GameMatchDTO dto = GameMatchDTO.fromEntity(gameMatch);

            assertNotNull(dto);
            assertEquals(1L, dto.getId());
            assertEquals(playerOneDTO, dto.getPlayerOne());
            assertEquals(playerTwoDTO, dto.getPlayerTwo());
            assertEquals(submissionOneDTO, dto.getSubmissionOne());
            assertEquals(submissionTwoDTO, dto.getSubmissionTwo());
            assertEquals(MATCH_STATUS.IN_PROGRESS, dto.getStatus());
            assertEquals(MATCH_REASON.LADDER, dto.getReason());
            assertEquals(createdAt, dto.getCreatedAt());
            assertEquals(processedAt, dto.getProcessedAt());
            assertEquals(2, dto.getTimesQueued());

            playerDTOMock.verify(() -> PlayerDTO.fromEntity(playerOne));
            playerDTOMock.verify(() -> PlayerDTO.fromEntity(playerTwo));
            submissionDTOMock.verify(() -> SubmissionDTO.fromEntity(submissionOne));
            submissionDTOMock.verify(() -> SubmissionDTO.fromEntity(submissionTwo));
        }
    }


}