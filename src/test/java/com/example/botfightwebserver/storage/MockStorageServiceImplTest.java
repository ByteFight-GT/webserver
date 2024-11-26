package com.example.botfightwebserver.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class MockStorageServiceImplTest {

    private MockStorageServiceImpl storageService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = LocalDateTime.of(2024, 1, 1, 12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant();
        fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

        storageService = new MockStorageServiceImpl(fixedClock);
    }

    @Test
    void uploadFile_ValidFile_ReturnsCorrectFormat() {
        // Arrange
        Long teamId = 123L;
        String filename = "test.txt";
        MultipartFile file = new MockMultipartFile(
            "file",
            filename,
            "text/plain",
            "test content".getBytes()
        );
        String result = storageService.uploadFile(teamId, file);

        String expected = "TEAM_123/test.txt_20240101120000";
        assertEquals(expected, result);
    }

    @Test
    void uploadFile_NullFilename_UsesUnknown() {
        Long teamId = 123L;
        MultipartFile file = new MockMultipartFile(
            "file",
            null,
            "text/plain",
            "test content".getBytes()
        );

        String result = storageService.uploadFile(teamId, file);

        String expected = "TEAM_123/unknown_20240101120000";
        assertEquals(expected, result);
    }

    @Test
    void uploadFile_NullFile_ThrowsException() {
        Long teamId = 123L;

        assertThrows(IllegalArgumentException.class, () ->
            storageService.uploadFile(teamId, null)
        );
    }

    @Test
    void uploadFile_EmptyFile_ThrowsException() {
        Long teamId = 123L;
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () ->
            storageService.uploadFile(teamId, file)
        );
    }
}