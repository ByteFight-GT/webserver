package org.bytefight.webserver.social.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import org.bytefight.webserver.social.domain.Profile;

@Getter
@Builder
public class PublicProfileDto {
    @NotNull String uuid;
    @NotNull String username;
    String description;
    @NotNull String major;
    @NotNull @Min(0) Integer year;


    public static PublicProfileDto from(Profile profile) {
        return PublicProfileDto.builder()
                .uuid(profile.getPlayer().getUser().getUuid().toString())
                .username(profile.getPlayer().getUsername())
                .description(profile.getDescription())
                .major(profile.getMajor())
                .year(profile.getYear())
                .build();
    }
    
}
