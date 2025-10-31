package com.example.botfightwebserver.databaseBackup;

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
public class DatabaseBackupService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;


    public String createDatabaseDump() {
        try {
            Process process = getProcess();

            String output = readStream(process.getInputStream());
            String errorOutput = readStream(process.getErrorStream());

            int exitValue = process.waitFor();

            if (exitValue != 0) {
                log.error("pg_dump process exited with error code {}: {}", exitValue, errorOutput);
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

        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "--host", dbHost,
                "--username", dbUser,
                "--port", dbPort,
                "--no-password", // avoid the password prompt and instead make with env variables
                dbName
        );
        Map<String, String> env = pb.environment();
        env.put("PGPASSWORD", dbPassword);

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
