package org.bytefight.webserver.turnstile.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "turnstile")
public record TurnstileProperties(boolean enabled, String secretKey) {}
