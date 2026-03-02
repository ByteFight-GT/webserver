package org.bytefight.webserver.auth.domain.dto;

import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
public class AdminUserWithPlayerDto {
  Long id;
  UUID uuid;
  String email;
  Instant createdAt;
  boolean isAdmin;
  Long playerId;
  String playerUsername;
}
