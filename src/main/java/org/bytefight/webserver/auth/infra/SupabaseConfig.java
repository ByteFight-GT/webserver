package org.bytefight.webserver.auth.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class SupabaseConfig {
    @Bean
    HttpClient supabaseHttpClient(SupabaseProperties p) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(p.connectTimeoutMs() == null ? 3000 : p.connectTimeoutMs()))
                .build();
    }
}
