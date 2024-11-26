package com.example.botfightwebserver.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GcpStorageServiceImplTest {

    @Mock
    private Storage storage;

    private GcpStorageServiceImpl storageService;
    private Clock fixedClock;
    private final String BUCKET_NAME = "botfight_submissions";

    @BeforeEach
    void setUp() {
        Instant fixedInstant = LocalDateTime.of(2024, 1, 1, 12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant();
        fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        storageService = new GcpStorageServiceImpl(storage, fixedClock);
    }

    @Test
    void uploadFile_Success() throws IOException {
        Long teamId = 123L;
        String originalFileName = "test.txt";
        String contentType = "text/plain";
        byte[] content = "test content".getBytes();

        MultipartFile file = new MockMultipartFile(
            "file",
            originalFileName,
            contentType,
            content
        );

        Blob mockBlob = mock(Blob.class);
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(mockBlob);

        String result = storageService.uploadFile(teamId, file);

        String expectedFileName = "TEAM_123/test.txt_20240101120000";
        assertEquals(expectedFileName, result);
        verify(storage, times(1)).create(any(BlobInfo.class), eq(content));
    }

    @Test
    void generateFileName_ReturnsCorrectFormat() {
        Long teamId = 123L;
        String originalFileName = "test.txt";

        String result = storageService.generateFileName(teamId, originalFileName);

        String expectedFileName = "TEAM_123/test.txt_20240101120000";
        assertEquals(expectedFileName, result);
    }

}