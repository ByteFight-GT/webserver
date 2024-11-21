package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.submission.STORAGE_SOURCE;
import com.example.botfightwebserver.submission.Submission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameMatchJobTest {

    @Mock
    private GameMatch gameMatch;

    @Mock
    private Submission submission1;

    @Mock
    private Submission submission2;

    @Test
    void testFromEntity() {
        when(gameMatch.getId()).thenReturn(1L);
        when(gameMatch.getSubmissionOne()).thenReturn(submission1);
        when(gameMatch.getSubmissionTwo()).thenReturn(submission2);
        when(gameMatch.getMap()).thenReturn("test_map");

        when(submission1.getStoragePath()).thenReturn("path/to/submission1");
        when(submission1.getSource()).thenReturn(STORAGE_SOURCE.GCP);

        when(submission2.getStoragePath()).thenReturn("path/to/submission2");
        when(submission2.getSource()).thenReturn(STORAGE_SOURCE.LOCAL);

        GameMatchJob job = GameMatchJob.fromEntity(gameMatch);

        assertNotNull(job);
        assertEquals(1L, job.gameMatchId());
        assertEquals("path/to/submission1", job.Submission1Path());
        assertEquals("path/to/submission2", job.Submission2Path());
        assertEquals(STORAGE_SOURCE.GCP, job.source1());
        assertEquals(STORAGE_SOURCE.LOCAL, job.source2());
        assertEquals("test_map", job.map());
    }

}