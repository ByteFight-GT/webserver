package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.gameMatch.application.GameMatchRescheduler;
import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.GameMatchJob;
import com.example.botfightwebserver.gameMatch.domain.MATCH_REASON;
import com.example.botfightwebserver.submission.STORAGE_SOURCE;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameMatchReschedulerTest {

    @Mock
    private GameMatchService gameMatchService;

    private GameMatchRescheduler gameMatchRescheduler;

    private List<GameMatchJob> jobs;

    @BeforeEach
    void setUp() {
        jobs = List.of(new GameMatchJob(1L, "path1", "path2", STORAGE_SOURCE.GCP, STORAGE_SOURCE.GCP, MATCH_REASON.LADDER,"map"),
            new GameMatchJob(2L, "path_3", "path_4", STORAGE_SOURCE.GCP, STORAGE_SOURCE.GCP, MATCH_REASON.LADDER,"map"));

        when(gameMatchService.rescheduleFailedAndStaleMatches(false)).thenReturn(jobs);
        gameMatchRescheduler = new GameMatchRescheduler(gameMatchService);
    }

    @Test
    void reschedule() {
        List<GameMatchJob> returnedJobs = gameMatchRescheduler.reschedule();
        assertEquals(jobs, returnedJobs);
    }
}