package org.bytefight.webserver.databaseBackup.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backup")
public record BackupProperties(
    String dbDumpDir
) { }
