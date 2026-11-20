package org.bytefight.webserver.profile.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SuggestedProfileDto {
  @NotNull ProfileDto profile;
  @NotNull List<String> matchingFields;
}
