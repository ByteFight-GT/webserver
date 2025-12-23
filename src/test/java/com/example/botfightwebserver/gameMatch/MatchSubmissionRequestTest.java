package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.gameMatch.domain.MatchReason;
import com.example.botfightwebserver.gameMatch.domain.MatchSubmissionRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchSubmissionRequestTest {

    @Test
    void testGettersAndSetters() {
        MatchSubmissionRequest request = new MatchSubmissionRequest();

        request.setTeam1Id(1L);
        request.setTeam2Id(2L);
        request.setSubmission1Id(3L);
        request.setSubmission2Id(4L);
        request.setReason(MatchReason.LADDER);
        request.setMap("test_map");

        assertEquals(1L, request.getTeam1Id());
        assertEquals(2L, request.getTeam2Id());
        assertEquals(3L, request.getSubmission1Id());
        assertEquals(4L, request.getSubmission2Id());
        assertEquals(MatchReason.LADDER, request.getReason());
        assertEquals("test_map", request.getMap());
    }

    @Test
    void testEqualsAndHashCode() {
        MatchSubmissionRequest request1 = new MatchSubmissionRequest();
        MatchSubmissionRequest request2 = new MatchSubmissionRequest();

        request1.setTeam1Id(1L);
        request1.setReason(MatchReason.LADDER);

        request2.setTeam1Id(1L);
        request2.setReason(MatchReason.LADDER);

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }
}