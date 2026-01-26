package org.bytefight.webserver.competition.domain.dto;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.competition.domain.Competition;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompetitionDto {
    String slug;
    String name;
    String description;
    boolean isActive;
    boolean isWhitelisted;
    TimestampsDto timestamps;

    public static CompetitionDto from(Competition competition) {
        return CompetitionDto.builder()
                .slug(competition.getSlug())
                .name(competition.getName())
                .description(competition.getDescription())
                .isActive(competition.isActive())
                .isWhitelisted(competition.isWhitelisted())
                .timestamps(TimestampsDto.from(competition))
                .build();
    }
}
