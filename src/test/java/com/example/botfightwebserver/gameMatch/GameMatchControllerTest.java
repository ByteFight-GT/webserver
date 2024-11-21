package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.submission.STORAGE_SOURCE;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameMatchController.class)
class GameMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameMatchService gameMatchService;

    @Test
    void testSubmitMatch() throws Exception {
        // Arrange: Create an instance of MatchSubmissionRequest
        MatchSubmissionRequest request = new MatchSubmissionRequest();
        request.setPlayer1Id(1L);
        request.setPlayer2Id(2L);
        request.setSubmission1Id(10L);
        request.setSubmission2Id(20L);
        request.setReason(MATCH_REASON.VALIDATION);
        request.setMap("Map1");

        GameMatchJob job = new GameMatchJob(
                123L,
                "path/to/submission1",
                "path/to/submission2",
                STORAGE_SOURCE.LOCAL,
                STORAGE_SOURCE.LOCAL,
                "Map1"
        );

        when(gameMatchService.submitGameMatch(1L, 2L, 10L, 20L, MATCH_REASON.VALIDATION, "Map1"))
                .thenReturn(job);

        // Act: Perform the request
        mockMvc.perform(post("/api/v1/game-match/submit/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameMatchId").value(123))
                .andExpect(jsonPath("$.Submission1Path").value("path/to/submission1"))
                .andExpect(jsonPath("$.Submission2Path").value("path/to/submission2"))
                .andExpect(jsonPath("$.map").value("Map1"));

        // Assert: Verify interaction with service
        verify(gameMatchService).submitGameMatch(1L, 2L, 10L, 20L, MATCH_REASON.VALIDATION, "Map1");
    }

    @Test
    void testRemoveAllQueuedMatches() throws Exception {
        // Arrange: Mock service to return an empty list
        when(gameMatchService.deleteQueuedMatches()).thenReturn(List.of());

        // Act and Assert
        mockMvc.perform(post("/api/v1/game-match/queue/remove_all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(gameMatchService).deleteQueuedMatches();
    }

    @Test
    void testQueuedMatches() throws Exception {
        // Arrange: Mock the service to return a list of GameMatchJobs
        List<GameMatchJob> jobs = List.of(
                new GameMatchJob(123L, "path1", "path2", STORAGE_SOURCE.LOCAL, STORAGE_SOURCE.LOCAL, "Map1"),
                new GameMatchJob(124L, "path3", "path4", STORAGE_SOURCE.LOCAL, STORAGE_SOURCE.LOCAL, "Map2")
        );

        when(gameMatchService.peekQueuedMatches()).thenReturn(jobs);

        // Act and Assert
        mockMvc.perform(get("/api/v1/game-match/queued"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].gameMatchId").value(123))
                .andExpect(jsonPath("$[0].map").value("Map1"))
                .andExpect(jsonPath("$[1].gameMatchId").value(124))
                .andExpect(jsonPath("$[1].map").value("Map2"));

        verify(gameMatchService).peekQueuedMatches();
    }

    @Test
    void testRescheduleAllQueuedMatches() throws Exception {
        // Arrange: Mock the service to return a list of rescheduled GameMatchJobs
        List<GameMatchJob> rescheduledJobs = List.of(
                new GameMatchJob(125L, "reschedulePath1", "reschedulePath2", STORAGE_SOURCE.LOCAL, STORAGE_SOURCE.LOCAL, "RescheduleMap1")
        );

        when(gameMatchService.rescheduleFailedAndStaleMatches()).thenReturn(rescheduledJobs);

        // Act and Assert
        mockMvc.perform(post("/api/v1/game-match/reschedule/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].gameMatchId").value(125))
                .andExpect(jsonPath("$[0].map").value("RescheduleMap1"));

        verify(gameMatchService).rescheduleFailedAndStaleMatches();
    }
}
