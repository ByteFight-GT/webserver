package com.example.botfightwebserver.databaseBackup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/database-backup")
@Slf4j
public class DatabaseBackupController {
    private final DatabaseBackupService databaseBackupService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/backup")
    public ResponseEntity<String> createBackup() {
        try {
            log.info("Received request to create database backup. Attempting to create backup");
            String databaseDump = databaseBackupService.createDatabaseDump();
            return ResponseEntity.ok(databaseDump);
        } catch (RuntimeException e) {
            log.error("Backup failed. Controller is returning an error response.", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create backup. Check logs for more info");
        }
    }
}
