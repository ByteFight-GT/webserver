package com.example.botfightwebserver.databaseBackup.application;

import com.example.botfightwebserver.databaseBackup.infra.BackupProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseBackupService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final BackupProperties backupProperties;

    public String createDatabaseDump() {
        try {
            Process process = getProcess();

            String output = readStream(process.getInputStream());
            String errorOutput = readStream(process.getErrorStream());

            int exitValue = process.waitFor();

            if (exitValue != 0) {
                log.error("pg_dump process exited with error code {}. Full output" +
                        "(stdout): '{}'. Error output (stderr): '{}'", exitValue, output.trim(), errorOutput.trim());
                throw new RuntimeException("Database dump process failed.");
            }
            log.info("Database dump created successfully");
            return output;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            log.error("An internal exception occurred during backup process.", e);
            throw new RuntimeException("Failed to create backup due to an internal error.", e);
        }
    }

    private Process getProcess() throws URISyntaxException, IOException {
        URI dbUri = new URI(dbUrl.replace("jdbc:", ""));
        String dbHost = dbUri.getHost();
        String dbPort = String.valueOf(dbUri.getPort());
        String dbName = dbUri.getPath().substring(1);
        ProcessBuilder pb = new ProcessBuilder("./create_backup.sh");
        Map<String, String> env = pb.environment();
        env.put("DB_HOST", dbHost);
        env.put("DB_PORT", dbPort);
        env.put("DB_NAME", dbName);
        env.put("DB_USER", dbUser);
        env.put("DB_PASSWORD", dbPassword);
        env.put("BACKUP_DIR", backupProperties.dbDumpDir());

        Process process = pb.start();
        return process;
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ( (line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        }
        // java will automatically call reader.close() because we wrapped BufferedReader in the try block
        return content.toString();
    }
}
