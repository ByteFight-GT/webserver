package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateGameOutcomeReasonDto(
    @Size(max = 255) @Pattern(regexp = ".*\\S.*") String displayLabel, Boolean visible) {}
