package com.example.botfightwebserver.auth.application;

import com.example.botfightwebserver.auth.infra.SupabaseProperties;
import com.example.botfightwebserver.auth.domain.SupabaseDtos.*;
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
import java.util.List;
import java.util.Map;

@Service
public class SupabaseService {
    private final HttpClient http;
    private final ObjectMapper om;
    private final SupabaseProperties props;
    private final Duration readTimeout;

    public SupabaseService(HttpClient http, ObjectMapper om, SupabaseProperties props) {
        this.http = http;
        this.om = om;
        this.props = props;
        this.readTimeout = Duration.ofMillis(props.readTimeoutMs() == null ? 5000 : props.readTimeoutMs());
    }

    public SupabaseUser createUser(String email,
                                   String password,
                                   boolean emailConfirmed,
                                   Map<String, Object> userMetadata,
                                   Map<String, Object> appMetadata) {
        var body = new CreateUserRequest(email, password, emailConfirmed, userMetadata, appMetadata);
        return post("/auth/v1/admin/users", body, SupabaseUser.class);
    }

    public SupabaseMagicLink createMagicSignInLink(String email) {
        var body = new GenerateLinkRequest(email);
        return post("/auth/v1/admin/generate_link", body, SupabaseMagicLink.class);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        try {
            var req = base(path).POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return handle(resp, type);
        } catch (IOException | InterruptedException e) {
            throw new SupabaseServiceException("POST " + path + " failed", e);
        }
    }

    private <T> T post(String path, Object body, TypeReference<T> typeRef) {
        try {
            var req = base(path).POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return handle(resp, typeRef);
        } catch (IOException | InterruptedException e) {
            throw new SupabaseServiceException("POST " + path + " failed", e);
        }
    }

    private <T> T get(String path, Class<T> type) {
        try {
            var req = base(path).GET().build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return handle(resp, type);
        } catch (IOException | InterruptedException e) {
            throw new SupabaseServiceException("GET " + path + " failed", e);
        }
    }

    private <T> T get(String path, TypeReference<T> typeRef) {
        try {
            var req = base(path).GET().build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return handle(resp, typeRef);
        } catch (IOException | InterruptedException e) {
            throw new SupabaseServiceException("GET " + path + " failed", e);
        }
    }

    private void delete(String path) {
        try {
            var req = base(path).DELETE().build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            handleNoBody(resp);
        } catch (IOException | InterruptedException e) {
            throw new SupabaseServiceException("DELETE " + path + " failed", e);
        }
    }

    private HttpRequest.Builder base(String path) {
        if (props.projectUrl() == null || props.serviceRole() == null) {
            throw new IllegalStateException("Supabase projectUrl/serviceRole not configured");
        }
        return HttpRequest.newBuilder(URI.create(props.projectUrl() + path))
                .timeout(readTimeout)
                .header("Authorization", "Bearer " + props.serviceRole())
                .header("apikey", props.serviceRole());
    }

    private <T> T handle(HttpResponse<String> resp, Class<T> type) {
        var sc = resp.statusCode();
        var body = resp.body();
        if (sc >= 200 && sc < 300) {
            try {
                return om.readValue(body, type);
            } catch (Exception e) {
                throw new SupabaseServiceException("Parse error: " + body, e);
            }
        }
        throw toApiError(body, sc);
    }

    private <T> T handle(HttpResponse<String> resp, TypeReference<T> typeRef) {
        var sc = resp.statusCode();
        var body = resp.body();
        if (sc >= 200 && sc < 300) {
            try {
                return om.readValue(body, typeRef);
            } catch (Exception e) {
                throw new SupabaseServiceException("Parse error: " + body, e);
            }
        }
        throw toApiError(body, sc);
    }

    private void handleNoBody(HttpResponse<String> resp) {
        var sc = resp.statusCode();
        if (sc >= 200 && sc < 300) return;
        throw toApiError(resp.body(), sc);
    }

    private SupabaseServiceException toApiError(String body, int status) {
        try {
            var err = om.readValue(body, SupabaseError.class);
            return new SupabaseServiceException(err.bestMessage());
        } catch (Exception ignored) {
            return new SupabaseServiceException("Supabase " + status + ": " + body);
        }
    }

    public static class SupabaseServiceException extends RuntimeException {
        public SupabaseServiceException(String msg) {
            super(msg);
        }

        public SupabaseServiceException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
