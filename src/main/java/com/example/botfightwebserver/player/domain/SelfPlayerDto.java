package com.example.botfightwebserver.player.domain;

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
    @NotNull String name;
    @NotNull String email;
    String teamUuid;
    @NotNull List<String> badges;

    public static SelfPlayerDto from(Player player) {
        return SelfPlayerDto.builder()
                .uuid(player.getUser().getUuid().toString())
                .name(player.getUsername())
                .email(player.getUser().getEmail())
                .teamUuid(player.getTeam() != null ? player.getTeam().getUuid().toString() : null)
                .build();
    }
}
