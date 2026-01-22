package com.example.botfightwebserver.auth.domain.dto;

import com.example.botfightwebserver.auth.domain.User;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class SelfUserDto {
    UUID uuid;
    String email;
    Instant createdAt;
    boolean isAdmin;

    public static SelfUserDto from(User user) {
        return SelfUserDto.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .isAdmin(user.isAdmin())
                .build();
    }
}
