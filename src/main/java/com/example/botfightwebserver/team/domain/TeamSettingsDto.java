package com.example.botfightwebserver.team.domain;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class TeamSettingsDto {
    @NotNull String name;
    String quote;
    boolean displayMembers;
}
