package com.example.botfightwebserver.common.domain.dto;

import com.example.botfightwebserver.common.domain.BaseEntity;
import com.example.botfightwebserver.competition.domain.Competition;
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
