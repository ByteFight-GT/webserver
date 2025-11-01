package com.example.botfightwebserver.databaseBackup;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "backup")
public record BackupProperties(
    String dbDumpDir
) { }
