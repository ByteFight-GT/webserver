package com.example.botfightwebserver.player.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatePlayerProfileDto {
    @PlayerUsername
    private String username;
}
