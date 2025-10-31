package com.example.botfightwebserver.student.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentDto {
    @NotNull String id;
    @NotNull String email;
    @NotNull String playerName;
    @NotNull String teamUuid;
    @NotNull String teamName;
    @NotNull Double teamGlicko;
    @NotNull Integer teamRanking;
}
