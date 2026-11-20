package org.bytefight.webserver.profile.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.player.domain.Player;

@Value
@Builder
public class ProfileDto {
  @NotNull String playerUuid;
  @NotNull String username;
  String fullName;
  String description;
  String major;
  String school;
  Integer graduationYear;
  @NotNull TimestampsDto timestampsDto;

  public static ProfileDto from(Profile profile) {
    Player player = profile.getPlayer();
    return ProfileDto.builder()
        .playerUuid(player.getUser().getUuid().toString())
        .username(player.getUsername())
        .fullName(player.getFullName())
        .description(profile.getDescription())
        .major(profile.getMajor())
        .school(profile.getSchool())
        .graduationYear(profile.getGraduationYear())
        .timestampsDto(TimestampsDto.from(profile))
        .build();
  }
}
