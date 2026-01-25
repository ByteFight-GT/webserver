package com.example.botfightwebserver.player.domain;

import com.example.botfightwebserver.common.domain.dto.TimestampsDto;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class SelfPlayerDto {
    @NotNull String uuid;
    @NotNull String username;
    @NotNull TimestampsDto timestampsDto;

    public static SelfPlayerDto from(Player player) {
        return SelfPlayerDto.builder()
                .uuid(player.getUser().getUuid().toString())
                .username(player.getUsername())
                .timestampsDto(TimestampsDto.from(player))
                .build();
    }
}
