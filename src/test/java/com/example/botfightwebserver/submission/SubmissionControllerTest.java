//package com.example.botfightwebserver.submission;
//
//import com.example.botfightwebserver.gameMatch.GameMatch;
//import com.example.botfightwebserver.gameMatch.GameMatchJob;
//import com.example.botfightwebserver.gameMatch.GameMatchService;
//import com.example.botfightwebserver.gameMatch.MATCH_REASON;
//import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
//import com.example.botfightwebserver.team.Team;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.web.multipart.MultipartFile;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.Mockito.any;
//import static org.mockito.Mockito.argThat;
//import static org.mockito.Mockito.eq;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class SubmissionControllerTest {
//
//    @Mock
//    private SubmissionService submissionService;
//
//    @Mock
//    private GameMatchService gameMatchService;
//
//    @Mock
//    private RabbitMQService rabbitMQService;
//
//    @InjectMocks
//    private SubmissionController submissionController;
//
//    @Test
//    void uploadSubmission_Success() {
//        // Arrange
//        Long teamId = 123L;
//        MultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "test content".getBytes()
//        );
//
//
//        Submission mockSubmission = new Submission();
//        mockSubmission.setId(1L);
//        mockSubmission.setTeamId(teamId);
//        mockSubmission.setStoragePath("mock/file/path");
//        mockSubmission.setSubmissionValidity(SUBMISSION_VALIDITY.NOT_EVALUATED);
//
//        Team mockteam = new Team();
//        mockteam.setId(teamId);
//
//        GameMatch mockGameMatch = new GameMatch();
//        mockGameMatch.setTeamOne(mockteam);
//        mockGameMatch.setTeamTwo(mockteam);
//        mockGameMatch.setSubmissionOne(mockSubmission);
//        mockGameMatch.setSubmissionTwo(mockSubmission);
//        mockGameMatch.setReason(MATCH_REASON.VALIDATION);
//        mockGameMatch.setMap("val_map");
//
//        when(submissionService.createSubmission(teamId, file)).thenReturn(mockSubmission);
//        when(gameMatchService.createMatch(123L, 123L, 1L, 1L,  MATCH_REASON.VALIDATION, "val_map")).thenReturn(
//            mockGameMatch
//        );
//
//        // Act
//        ResponseEntity<SubmissionDTO> response = submissionController.uploadSubmission(teamId, file);
//
//        // Assert
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//
//        // Verify the service method was called
//        verify(submissionService, times(1)).createSubmission(eq(teamId), eq(file));
//        verify(gameMatchService, times(1)).createMatch(123L, 123L, 1L, 1L,  MATCH_REASON.VALIDATION, "val_map");
//        verify(rabbitMQService, times(1)).enqueueGameMatchJob(GameMatchJob.fromEntity(mockGameMatch));
//        // Add verification that prints when service is called
//        verify(submissionService, times(1)).createSubmission(
//                argThat(pid -> {
//                    return pid.equals(teamId);
//                }),
//                any()
//        );
//    }
//}