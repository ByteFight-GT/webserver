package com.example.botfightwebserver.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {
    private Long id;
    private String name;
    private String email;
    private Long teamId;
    private Boolean hasTeam;
    private List<String> badges;
    private LocalDateTime creationDateTime;

    public static PlayerDTO fromEntity(Player player) {
        return PlayerDTO.builder()
            .id(player.getId())
            .name(player.getName())
            .email(player.getEmail())
            .teamId(player.getTeamId())
            .hasTeam(player.isHasTeam())
            .creationDateTime(player.getCreationDateTime())
            .badges(player.getBadgeList())
            .build();
    }
}
