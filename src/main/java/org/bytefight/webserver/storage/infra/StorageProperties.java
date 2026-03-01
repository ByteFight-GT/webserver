package org.bytefight.webserver.storage.infra;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(Path root, String hmacSecret) {}
