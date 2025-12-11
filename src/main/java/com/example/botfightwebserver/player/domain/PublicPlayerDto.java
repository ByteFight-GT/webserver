package com.example.botfightwebserver.player.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class PublicPlayerDto {
    String uuid;
    String username;
    String teamUuid;
    List<String> badges;
    LocalDateTime creationDateTime;

    public static PublicPlayerDto from(Player player) {
        return PublicPlayerDto.builder()
                .uuid(player.getUser().getUuid().toString())
                .username(player.getUsername())
                .teamUuid(player.getTeam().getUuid().toString())
                .build();
    }
}
