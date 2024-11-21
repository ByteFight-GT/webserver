package com.example.botfightwebserver.gameMatch;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchSubmissionRequestTest {

    @Test
    void testGettersAndSetters() {
        MatchSubmissionRequest request = new MatchSubmissionRequest();

        request.setPlayer1Id(1L);
        request.setPlayer2Id(2L);
        request.setSubmission1Id(3L);
        request.setSubmission2Id(4L);
        request.setReason(MATCH_REASON.LADDER);
        request.setMap("test_map");

        assertEquals(1L, request.getPlayer1Id());
        assertEquals(2L, request.getPlayer2Id());
        assertEquals(3L, request.getSubmission1Id());
        assertEquals(4L, request.getSubmission2Id());
        assertEquals(MATCH_REASON.LADDER, request.getReason());
        assertEquals("test_map", request.getMap());
    }

    @Test
    void testEqualsAndHashCode() {
        MatchSubmissionRequest request1 = new MatchSubmissionRequest();
        MatchSubmissionRequest request2 = new MatchSubmissionRequest();

        request1.setPlayer1Id(1L);
        request1.setReason(MATCH_REASON.LADDER);

        request2.setPlayer1Id(1L);
        request2.setReason(MATCH_REASON.LADDER);

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }
}