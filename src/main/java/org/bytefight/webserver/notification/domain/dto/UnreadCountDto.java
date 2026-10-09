package org.bytefight.webserver.notification.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class UnreadCountDto {
  @NotNull long count;
}
