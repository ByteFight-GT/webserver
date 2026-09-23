package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateGameOutcomeReasonDto(
    @NotNull Long competitionId,
    @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9_]+$") String code,
    @NotBlank @Size(max = 255) String displayLabel) {}
