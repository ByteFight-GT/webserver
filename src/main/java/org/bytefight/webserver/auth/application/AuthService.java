package org.bytefight.webserver.auth.application;

import org.bytefight.webserver.auth.domain.dto.SupabaseDtos;
import org.bytefight.webserver.auth.infra.AuthProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
public class AuthService {
    private final HttpClient http;
    private final ObjectMapper om;
    private final AuthProperties props;
    private final Duration readTimeout;

    public AuthService(HttpClient http, ObjectMapper om, AuthProperties props) {
        this.http = http;
        this.om = om;
        this.props = props;
        // Default to 5 seconds if no timeout is specified in properties
        this.readTimeout = Duration.ofMillis(props.readTimeoutMs() == null ? 5000 : props.readTimeoutMs());
    }

    public SupabaseDtos.SupabaseUser createUser(String email,
                                                String password,
                                                boolean emailConfirmed,
                                                Map<String, Object> userMetadata,
                                                Map<String, Object> appMetadata) {
        var body = new SupabaseDtos.CreateUserRequest(email, password, emailConfirmed, userMetadata, appMetadata);
        return post("/admin/users", body, SupabaseDtos.SupabaseUser.class);
    }

    public SupabaseDtos.SupabaseMagicLink createMagicSignInLink(String email) {
        var body = new SupabaseDtos.GenerateLinkRequest(email);
        return post("/admin/generate_link", body, SupabaseDtos.SupabaseMagicLink.class);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        try {
            var jsonBody = om.writeValueAsString(body);
            var req = base(path)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return handle(resp, type);
        } catch (IOException | InterruptedException e) {
            throw new AuthServiceException("POST " + path + " failed", e);
        }
    }

    private HttpRequest.Builder base(String path) {
        if (props.baseUrl() == null || props.adminSecret() == null) {
            throw new IllegalStateException("Auth baseUrl/adminSecret not configured");
        }

        return HttpRequest.newBuilder(URI.create(props.baseUrl() + path))
                .timeout(readTimeout)
                .header("Authorization", "Bearer " + props.adminSecret())
                .header("apikey", props.adminSecret());
    }

    private <T> T handle(HttpResponse<String> resp, Class<T> type) {
        var sc = resp.statusCode();
        var body = resp.body();
        if (sc >= 200 && sc < 300) {
            try {
                return om.readValue(body, type);
            } catch (Exception e) {
                throw new AuthServiceException("Parse error: " + body, e);
            }
        }
        throw toApiError(body, sc);
    }

    private AuthServiceException toApiError(String body, int status) {
        try {
            var err = om.readValue(body, SupabaseDtos.SupabaseError.class);
            return new AuthServiceException(err.bestMessage());
        } catch (Exception ignored) {
            return new AuthServiceException("Auth Provider Error " + status + ": " + body);
        }
    }

    public static class AuthServiceException extends RuntimeException {
        public AuthServiceException(String msg) { super(msg); }
        public AuthServiceException(String msg, Throwable cause) { super(msg, cause); }
    }
}
