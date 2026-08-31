package org.bytefight.webserver.player.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.player.domain.Player;

@Value
@Builder
public class AdminPlayerDto {
  @NotNull Long id;
  @NotNull Long userId;
  @NotNull String uuid;
  @NotNull String username;
  @NotNull TimestampsDto timestampsDto;

  public static AdminPlayerDto from(Player player) {
    return AdminPlayerDto.builder()
        .id(player.getId())
        .userId(player.getUser().getId())
        .uuid(player.getUser().getUuid().toString())
        .username(player.getUsername())
        .timestampsDto(TimestampsDto.from(player))
        .build();
  }
}
