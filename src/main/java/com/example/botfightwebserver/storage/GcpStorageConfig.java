package com.example.botfightwebserver.storage;

import org.springframework.beans.factory.annotation.Value;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class GcpStorageConfig {

    @Value("${spring.cloud.gcp.credentials.encoded}")
    private String credentials;

    @Value("${spring.cloud.gcp.credentials.project}")
    private String projectId;

    protected FileInputStream createFileInputStream(String path) throws IOException {
        return new FileInputStream(path);
    }

    @Bean
    public Storage googleCloudStorage() throws IOException {
        byte[] decodedCredentials = Base64.getDecoder().decode(credentials);
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ByteArrayInputStream(decodedCredentials)
        );

        return StorageOptions.newBuilder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .build()
            .getService();    }
}
