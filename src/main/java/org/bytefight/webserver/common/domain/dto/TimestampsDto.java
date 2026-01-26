package org.bytefight.webserver.common.domain.dto;

import org.bytefight.webserver.common.domain.BaseEntity;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class TimestampsDto {
    Instant createdAt;
    Instant updatedAt;

    public static TimestampsDto from(BaseEntity baseEntity) {
        return TimestampsDto.builder()
                .createdAt(baseEntity.getCreatedAt())
                .updatedAt(baseEntity.getUpdatedAt())
                .build();
    }
}
