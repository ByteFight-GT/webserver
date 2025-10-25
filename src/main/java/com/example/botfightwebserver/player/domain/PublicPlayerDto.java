package com.example.botfightwebserver.player.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class PublicPlayerDto {
    String uuid;
    String name;
    String teamUuid;
    boolean hasTeam;
    List<String> badges;
    LocalDateTime creationDateTime;

    public static PublicPlayerDto from(Player player) {
        return PublicPlayerDto.builder()
                .uuid(player.getUser().getUuid().toString())
                .name(player.getName())
                .teamUuid(player.getTeam().getUuid().toString())
                .hasTeam(player.isHasTeam())
                .creationDateTime(player.getCreationDateTime())
                .badges(player.getBadgeList())
                .build();
    }
}
