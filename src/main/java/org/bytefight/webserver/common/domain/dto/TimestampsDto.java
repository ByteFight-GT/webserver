package org.bytefight.webserver.common.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import org.bytefight.webserver.common.domain.BaseEntity;

@Value
@Builder
public class TimestampsDto {
  @NotNull Instant createdAt;
  @NotNull Instant updatedAt;

  public static TimestampsDto from(BaseEntity baseEntity) {
    return TimestampsDto.builder()
        .createdAt(baseEntity.getCreatedAt())
        .updatedAt(baseEntity.getUpdatedAt())
        .build();
  }
}
