package com.example.botfightwebserver.submission;

import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamRepository;
import com.example.botfightwebserver.storage.StorageService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private SubmissionService submissionService;

    private Team testTeam;
    private Submission testSubmission;
    private MockMultipartFile validFile;
    private MockMultipartFile invalidFile;

    @BeforeEach
    void setUp() {
        // Set up test team
        testTeam = new Team();
        testTeam.setId(1L);

        // Set up test submission
        testSubmission = new Submission();
        testSubmission.setId(1L);
        testSubmission.setTeamId(1L);
        testSubmission.setStoragePath("test/path");
        testSubmission.setSubmissionValidity(SUBMISSION_VALIDITY.NOT_EVALUATED);

        // Set up valid test file
        validFile = new MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                "test content".getBytes()
        );

        // Set up invalid test file
        invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );
    }

    @Test
    void createSubmission_Success() {
        // Arrange
        when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
        when(storageService.uploadFile(eq(1L), any(MultipartFile.class))).thenReturn("test/path");
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        // Act
        Submission result = submissionService.createSubmission(1L, validFile);

        // Assert
        assertNotNull(result);
        assertEquals(testSubmission.getId(), result.getId());
        assertEquals(testSubmission.getTeamId(), result.getTeamId());
        assertEquals(testSubmission.getStoragePath(), result.getStoragePath());
        assertEquals(SUBMISSION_VALIDITY.NOT_EVALUATED, result.getSubmissionValidity());

        verify(teamRepository).findById(1L);
        verify(storageService).uploadFile(eq(1L), any(MultipartFile.class));
        verify(submissionRepository).save(any(Submission.class));
    }

    @Test
    void createSubmission_TeamNotFound() {
        // Arrange
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> submissionService.createSubmission(1L, validFile));

        verify(teamRepository).findById(1L);
        verifyNoInteractions(storageService);
        verifyNoInteractions(submissionRepository);
    }

    @Test
    void validateFile_ValidZipFile() {
        // Act & Assert
        assertDoesNotThrow(() -> submissionService.validateFile(validFile));
    }

    @Test
    void validateFile_EmptyFile() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.zip",
                "application/zip",
                new byte[0]
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> submissionService.validateFile(emptyFile));
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void validateFile_TooLarge() {
        // Arrange
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.zip",
                "application/zip",
                new byte[51 * 1024 * 1024] // Slightly over 50MB
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> submissionService.validateFile(largeFile));
        assertEquals("File is too large", exception.getMessage());
    }

    @Test
    void validateFile_InvalidContentType() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> submissionService.validateFile(invalidFile));
        assertEquals("Unsupported content type: only zip files allowed", exception.getMessage());
    }

    @Test
    void validateSubmissions_Success() {
        // Arrange
        when(submissionRepository.existsById(1L)).thenReturn(true);
        when(submissionRepository.existsById(2L)).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> submissionService.validateSubmissions(1L, 2L));
        verify(submissionRepository).existsById(1L);
        verify(submissionRepository).existsById(2L);
    }

    @Test
    void validateSubmissions_SameSubmission() {
        // Arrange
        Long submissionId = 1L;
        when(submissionRepository.existsById(submissionId)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> submissionService.validateSubmissions(submissionId, submissionId));
        assertEquals("Submission 1 is the same as submission 2", exception.getMessage());

        // Verify - the method calls existsById twice, once for each submission
        verify(submissionRepository, times(2)).existsById(submissionId);
    }

    @Test
    void validateSubmissions_NonExistentSubmission() {
        // Arrange
        when(submissionRepository.existsById(1L)).thenReturn(true);
        when(submissionRepository.existsById(2L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> submissionService.validateSubmissions(1L, 2L));
        assertEquals("Submission 1 or 2 does not exist", exception.getMessage());
    }

    @Test
    void validateSubmissionAfterMatch_Success() {
        // Arrange
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        // Act
        submissionService.validateSubmissionAfterMatch(1L);

        // Assert
        verify(submissionRepository).findById(1L);
        verify(submissionRepository).save(any(Submission.class));
        assertEquals(SUBMISSION_VALIDITY.VALID, testSubmission.getSubmissionValidity());
    }

    @Test
    void isSubmissionValid_ValidSubmission() {
        // Arrange
        testSubmission.setSubmissionValidity(SUBMISSION_VALIDITY.VALID);
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(testSubmission));

        // Act
        boolean result = submissionService.isSubmissionValid(1L);

        // Assert
        assertTrue(result);
        verify(submissionRepository).findById(1L);
    }

    @Test
    void isSubmissionValid_InvalidSubmission() {
        // Arrange
        testSubmission.setSubmissionValidity(SUBMISSION_VALIDITY.NOT_EVALUATED);
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(testSubmission));

        // Act
        boolean result = submissionService.isSubmissionValid(1L);

        // Assert
        assertFalse(result);
        verify(submissionRepository).findById(1L);
    }

    @Test
    void isSubmissionValid_SubmissionNotFound() {
        // Arrange
        when(submissionRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = submissionService.isSubmissionValid(1L);

        // Assert
        assertFalse(result);
        verify(submissionRepository).findById(1L);
    }

    @Test
    void getSubmissionReferenceById_Success() {
        // Arrange
        when(submissionRepository.getReferenceById(1L)).thenReturn(testSubmission);

        // Act
        Submission result = submissionService.getSubmissionReferenceById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testSubmission.getId(), result.getId());
        verify(submissionRepository).getReferenceById(1L);
    }
}