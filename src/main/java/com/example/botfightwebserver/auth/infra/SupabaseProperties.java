package com.example.botfightwebserver.auth.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(
        String projectUrl,
        String serviceRole,
        Integer connectTimeoutMs,
        Integer readTimeoutMs
) { }
