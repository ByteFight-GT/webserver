package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.security.JwtAuthFilter;
import com.example.botfightwebserver.submission.STORAGE_SOURCE;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.botfightwebserver.security.TestSecurityConfig;

@WebMvcTest(GameMatchController.class)
@WithMockUser(roles = "ADMIN")
@AutoConfigureMockMvc
class GameMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameMatchService gameMatchService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private static final String TEST_TOKEN = "5EBZXvyjCEspndvK/18edD7qHwXuy7H+HLOiYeDEQz4=";

    @Test
    void testSubmitMatch() throws Exception {
        // Arrange: Create an instance of MatchSubmissionRequest
        MatchSubmissionRequest request = new MatchSubmissionRequest();
        request.setTeam1Id(1L);
        request.setTeam2Id(2L);
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
                MATCH_REASON.LADDER,
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
        when(gameMatchService.deleteQueuedMatches()).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/game-match/queue/remove_all")
                .header("Authorization", TEST_TOKEN))  // Add auth header
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(gameMatchService).deleteQueuedMatches();
    }

    @Test
    void testQueuedMatches() throws Exception {
        // Arrange: Mock the service to return a list of GameMatchJobs
        List<GameMatchJob> jobs = List.of(
            new GameMatchJob(123L, "path1", "path2", STORAGE_SOURCE.LOCAL, STORAGE_SOURCE.LOCAL, MATCH_REASON.LADDER,
                "Map1"),
            new GameMatchJob(124L, "path3", "path4", STORAGE_SOURCE.LOCAL, STORAGE_SOURCE.LOCAL, MATCH_REASON.LADDER,
                "Map2")
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
            new GameMatchJob(125L, "reschedulePath1", "reschedulePath2", STORAGE_SOURCE.LOCAL, STORAGE_SOURCE.LOCAL,
                MATCH_REASON.LADDER, "RescheduleMap1")
        );

        when(gameMatchService.rescheduleFailedAndStaleMatches()).thenReturn(rescheduledJobs);

        // Act and Assert
        mockMvc.perform(post("/api/v1/game-match/reschedule/all")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andDo(result -> {
                System.out.println("Request URL: " + result.getRequest().getRequestURL());
                System.out.println("Request Method: " + result.getRequest().getMethod());
            });

        verify(gameMatchService).rescheduleFailedAndStaleMatches();
    }

    @Test
    void shouldHitEndpoint() throws Exception {
        // Add simple debug endpoint test
        mockMvc.perform(get("/api/v1/game-match/queued"))  // Try the non-secured endpoint first
            .andDo(result -> {
                System.out.println("Request URL: " + result.getRequest().getRequestURL());
                System.out.println("Handler: " + result.getHandler()); // This will show if Spring found a handler
            })
            .andDo(print());
    }


    @Test
    void shouldHitEndpoint2() throws Exception {
        List<GameMatchJob> jobs = List.of(new GameMatchJob(1L, "path1", "path2",
            STORAGE_SOURCE.LOCAL, STORAGE_SOURCE.LOCAL, MATCH_REASON.LADDER, "map1"));
        when(gameMatchService.peekQueuedMatches()).thenReturn(jobs);

        mockMvc.perform(get("/api/v1/game-match/queued")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());
    }
}
