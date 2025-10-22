package com.example.botfightwebserver.storage.domain;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
@ConfigurationProperties(prefix = "storage")
@Getter
public class StorageProperties {
    private Path root;
    private String baseUrl;
    private String hmacSecret;
}
