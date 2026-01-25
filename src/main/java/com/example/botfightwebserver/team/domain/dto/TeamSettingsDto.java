package com.example.botfightwebserver.team.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class TeamSettingsDto {
    @NotNull String name;
    String quote;

    @Builder.Default
    Boolean displayMembers = false;
}
