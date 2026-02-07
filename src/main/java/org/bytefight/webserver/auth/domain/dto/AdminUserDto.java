package org.bytefight.webserver.auth.domain.dto;

import lombok.Builder;
import lombok.Value;
import org.bytefight.webserver.auth.domain.User;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AdminUserDto {
    Long id;
    UUID uuid;
    String email;
    Instant createdAt;
    boolean isAdmin;

    public static AdminUserDto from(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .isAdmin(user.isAdmin())
                .build();
    }
}
