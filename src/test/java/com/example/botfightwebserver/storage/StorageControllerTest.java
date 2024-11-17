package com.example.botfightwebserver.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StorageControllerTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private StorageController storageController;

    @Test
    void verifyStorage_Success() {
        doNothing().when(storageService).verifyAccess();

        ResponseEntity<String> response = storageController.verifyStorage();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Can Access bucket", response.getBody());
        verify(storageService).verifyAccess();
    }

    @Test
    void verifyStorage_ThrowsException() {
        doThrow(new RuntimeException("Cannot access bucket"))
            .when(storageService).verifyAccess();

        try {
            storageController.verifyStorage();
        } catch (RuntimeException e) {
            assertEquals("Cannot access bucket", e.getMessage());
        }
        verify(storageService).verifyAccess();
    }
}