package org.bytefight.webserver.auth.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

import org.bytefight.webserver.user.domain.User;

@Value
@Builder
public class SelfAuthUserDto {
  UUID uuid;
  String email;
  Instant createdAt;
  boolean isAdmin;
  @NotNull Instant lastAcceptedTos;

  public static SelfAuthUserDto from(User user) {
    return SelfAuthUserDto.builder()
        .uuid(user.getUuid())
        .email(user.getEmail())
        .createdAt(user.getCreatedAt())
        .isAdmin(user.isAdmin())
        .lastAcceptedTos(user.getLastAcceptedTos())
        .build();
  }
}
