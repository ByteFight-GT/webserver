package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GameOutcomeReasonManifestDto(@NotNull @Valid List<Entry> reasons) {
  public record Entry(
      @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9_]+$") String code,
      @NotBlank @Size(max = 255) String defaultLabel) {}
}
