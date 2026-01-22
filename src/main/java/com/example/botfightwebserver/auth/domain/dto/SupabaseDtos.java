package com.example.botfightwebserver.auth.domain.dto;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;

public class SupabaseDtos {
    public record CreateUserRequest(
            String email,
            String password,
            Boolean email_confirm,
            Map<String,Object> user_metadata,
            Map<String,Object> app_metadata
    ) {}

    @Getter
    public static class GenerateLinkRequest {
        private String email;
        private String type = "magiclink";
        private String redirect_to = "https://bytefight.org/auth/callback";

        public GenerateLinkRequest(String email) {
            this.email = email;
        }
    }

    // Responses (subset of fields commonly used)
    public record SupabaseUser(
            String id,                      // UUID
            String email,
            Instant created_at,
            Map<String,Object> user_metadata,
            Map<String,Object> app_metadata
    ) {}

    public record SupabaseMagicLink(
            String id,                      // UUID
            String email,
            String action_link
    ) {}

    public record SupabaseError(String msg, String error_code) {
        public String bestMessage() {
            if (msg != null && !msg.isBlank()) return msg;
            if (error_code != null && !error_code.isBlank()) return error_code;
            return "Unknown Supabase error";
        }
    }
}
