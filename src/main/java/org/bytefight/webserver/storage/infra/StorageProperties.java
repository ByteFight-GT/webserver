package org.bytefight.webserver.storage.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
    Path root,
    String hmacSecret
) { }
