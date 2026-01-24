package com.example.botfightwebserver.competition.domain.dto;

import com.example.botfightwebserver.common.domain.dto.TimestampsDto;
import com.example.botfightwebserver.competition.domain.Competition;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompetitionDto {
    String slug;
    String name;
    String description;
    boolean isActive;
    TimestampsDto timestamps;

    public static CompetitionDto from(Competition competition) {
        return CompetitionDto.builder()
                .slug(competition.getSlug())
                .name(competition.getName())
                .description(competition.getDescription())
                .isActive(competition.isActive())
                .timestamps(TimestampsDto.from(competition))
                .build();
    }
}
