package com.example.botfightwebserver.player.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class SelfPlayerDto {
    String uuid;
    String name;
    String email;
    long teamId;
    boolean hasTeam;
    List<String> badges;
    LocalDateTime creationDateTime;

    public static SelfPlayerDto from(Player player) {
        return SelfPlayerDto.builder()
                .uuid(player.getUser().getUuid().toString())
                .name(player.getName())
                .email(player.getUser().getEmail())
                .teamId(player.getTeamId())
                .hasTeam(player.isHasTeam())
                .creationDateTime(player.getCreationDateTime())
                .badges(player.getBadgeList())
                .build();
    }
}
