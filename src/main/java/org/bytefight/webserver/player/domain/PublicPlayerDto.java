package org.bytefight.webserver.player.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;

@Value
@Builder
public class PublicPlayerDto {
  @NotNull String uuid;
  @NotNull String username;
  @NotNull TimestampsDto timestampsDto;

  public static PublicPlayerDto from(Player player) {
    return PublicPlayerDto.builder()
        .uuid(player.getUser().getUuid().toString())
        .username(player.getUsername())
        .timestampsDto(TimestampsDto.from(player))
        .build();
  }
}
