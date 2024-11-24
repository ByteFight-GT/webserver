package com.example.botfightwebserver.submission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private SubmissionController submissionController;

    @Test
    void uploadSubmission_Success() {
        // Arrange
        Long playerId = 123L;
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        Submission mockSubmission = new Submission();
        mockSubmission.setId(1L);
        mockSubmission.setPlayerId(playerId);
        mockSubmission.setStoragePath("mock/file/path");
        mockSubmission.setSubmissionValidity(SUBMISSION_VALIDITY.NOT_EVALUATED);

        when(submissionService.createSubmission(playerId, file)).thenReturn(mockSubmission);

        // Add debug statement before the controller call
        System.out.println("About to call controller");

        // Act
        ResponseEntity<SubmissionDTO> response = submissionController.uploadSubmission(playerId, file);

        // Add debug statement after the controller call
        System.out.println("Controller call completed");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the service method was called
        verify(submissionService, times(1)).createSubmission(eq(playerId), eq(file));

        // Add verification that prints when service is called
        verify(submissionService, times(1)).createSubmission(
                argThat(pid -> {
                    System.out.println("Service called with playerId: " + pid);
                    return pid.equals(playerId);
                }),
                any()
        );
    }
}