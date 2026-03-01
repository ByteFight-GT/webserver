package org.bytefight.webserver.auth.infra;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfiguration {
  @Bean
  HttpClient supabaseHttpClient(AuthProperties p) {
    return HttpClient.newBuilder()
        .connectTimeout(
            Duration.ofMillis(p.connectTimeoutMs() == null ? 3000 : p.connectTimeoutMs()))
        .build();
  }
}
