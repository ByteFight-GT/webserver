package org.bytefight.webserver.auth.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String baseUrl,
        String adminSecret,
        Integer connectTimeoutMs,
        Integer readTimeoutMs
) { }
