package org.bytefight.webserver.auth.domain.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

import org.bytefight.webserver.auth.domain.User;

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
